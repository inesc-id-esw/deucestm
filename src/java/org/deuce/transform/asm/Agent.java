package org.deuce.transform.asm;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.deuce.reflection.UnsafeHolder;
import org.deuce.transaction.ContextDelegator;
import org.deuce.transform.Exclude;

/**
 * A java agent to dynamically instrument transactional supported classes/  
 * 
 * Modified by FMC@2012-09-14:
 * * Included support for ClassEnhancer objects, which act like ASM transformations
 *   that we can add after the standard instrumentation made by the DeuceStm 
 *   to transactional classes.
 *    
 * @author Guy Korland
 * @since 1.0
 */
@Exclude
public class Agent implements ClassFileTransformer {
	final private static Logger logger = Logger.getLogger("org.deuce.agent");
	final private static boolean VERBOSE = Boolean.getBoolean("org.deuce.verbose");
	final private static boolean GLOBAL_TXN = Boolean.getBoolean("org.deuce.transaction.global");

	/**
	 * Objects providing a pre and post transformation.
	 */
	final private static ClassEnhancer postEnhancer, preEnhancer;

	static{
		try {
			/*
			 * Here, we force to load the ContextDelegator specified by the end user, because it may depends
			 * on post transformations.  
			 * In this case the specified ContextDelegator must define the required post transformations 
			 * in the corresponding system property: org.deuce.transform.post.   
			 * Then the Agent will process this transformation. 
			 */
			logger.info("context delegator = " + ContextDelegator.CONTEXT_DELEGATOR_INTERNAL);
			/*
			 * Loads the pre ClassEnhancer objects.  
			 */
			String enhancer = System.getProperty("org.deuce.transform.pre");
			ClassEnhancerChain aux = null;
			if(enhancer != null && !enhancer.equals("")){
				for(String eh : enhancer.split(",")){
					Class<ClassEnhancer> klassEnhancer = (Class<ClassEnhancer>) Class.forName(eh);
					aux = new ClassEnhancerChain(klassEnhancer, aux);
				}
			}
			preEnhancer = aux;
			/*
			 * Loads the post ClassEnhancer objects. 
			 */
			enhancer = System.getProperty("org.deuce.transform.post");
			if(enhancer != null && !enhancer.equals("")){
				for(String eh : enhancer.split(",")){
					/*
					 * The ClassEnhancerChain implement a chain of transformations. 
					 * This solution was adapted from the the Decorator design pattern. 
					 * We cannot make the own implementations of the ClassEnhancer as 
					 * decorators, because we need to instantiate a new ClassEnhancer 
					 * for each instrumentation.  
					 */
					Class<ClassEnhancer> klassEnhancer = (Class<ClassEnhancer>) Class.forName(eh);
					aux = new ClassEnhancerChain(klassEnhancer, aux);
				}
			}
			postEnhancer = aux;                
		} 
		catch (ClassNotFoundException e) {throw new RuntimeException(e);} 
	}

	/*
	 * @see java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader,
	 *      java.lang.String, java.lang.Class, java.security.ProtectionDomain,
	 *      byte[])
	 */
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer)
	throws IllegalClassFormatException {
		try {
			// Don't transform classes from the boot classLoader.
			if (loader != null){
				List<ClassByteCode> res = transform(className, classfileBuffer, false);
				for (int i = 1; i < res.size(); i++) {
					loadClass(res.get(i).getClassName(), res.get(i).getBytecode());
				}
				return res.get(0).getBytecode();
			}
		}
		catch(Exception e) {
			logger.log( Level.SEVERE, "Fail on class transform: " + className, e);
		}
		return classfileBuffer;
	}
	
 	/**
 	 * (FMC@203-01-08) Loads new classes included by the transformation.
 	 * E.g. the enhancer EnhanceStaticFields produces an auxiliary class containing 
 	 * transactional instance fields to replace the original static fields.  
 	 */
	private Class<?> loadClass(String className, byte[] b) {
		//override classDefine (as it is protected) and define the class.
		Class<?> clazz = null;
		try {
			ClassLoader loader = ClassLoader.getSystemClassLoader();
			Class<?> cls = Class.forName("java.lang.ClassLoader");
			java.lang.reflect.Method method =
					cls.getDeclaredMethod("defineClass", new Class[] { String.class, byte[].class, int.class, int.class });

			// protected method invocaton
			method.setAccessible(true);
			try {
				Object[] args = new Object[] { className.replace('/', '.'), b, new Integer(0), new Integer(b.length)};
				clazz = (Class) method.invoke(loader, args);
			} finally {
				method.setAccessible(false);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return clazz;
	}

	/**
	 * (FMC@2012-09-14) After performing the Deuce transformation we check the postEnhancer field.
	 * If the user specified a post transformation then we will instrument also every class resulted 
	 * from the Deuce transformation with the enhancement defined by the postEnhancer.
	 * 
	 * (FMC@2013-01-08) Before performing the Deuce transformation we check the preEnhancer field.
	 * If the user specified a pre transformation then we should apply it to the original classFileBuffer 
	 * and then we perform the Deuce transformation to every class returned by the preEnhancer process.
	 * 
	 * @param offline <code>true</code> if this is an offline transform.
	 */
	private List<ClassByteCode> transform(String className, byte[] classfileBuffer, boolean offline)
	throws IllegalClassFormatException {

		ArrayList<ClassByteCode> byteCodes = new ArrayList<ClassByteCode>();
		if (className.startsWith("$") || ExcludeIncludeStore.exclude(className)){
			byteCodes.add(new ClassByteCode( className, classfileBuffer));
			return byteCodes;
		}
		
		if (logger.isLoggable(Level.FINER))
			logger.finer("Transforming: Class=" + className);

		classfileBuffer = addFrames(className, classfileBuffer);

		if( GLOBAL_TXN){
			ByteCodeVisitor cv = new org.deuce.transaction.global.ClassTransformer( className);
			byte[] bytecode = cv.visit(classfileBuffer);
			byteCodes.add(new ClassByteCode( className, bytecode));
		}
		else{
 			/*
			 * Pre Deuce transformation
			 */
			List<ClassByteCode> preByteCodes = new LinkedList<ClassByteCode>();
			if(preEnhancer != null){
				preByteCodes = preEnhancer.visit(offline, className, classfileBuffer);
			}else{
				preByteCodes.add(new ClassByteCode( className, classfileBuffer));
			}
			/*
			 * Standard Deuce transformation
			 */
			for (ClassByteCode cb : preByteCodes) {
				ExternalFieldsHolder fieldsHolder = null;
				if(offline) {
					fieldsHolder = new ExternalFieldsHolder(cb.getClassName());
				}
				ClassTransformer cv = new ClassTransformer( cb.getClassName(), fieldsHolder);
				byte[] bytecode = cv.visit(cb.getBytecode());
				/*
				 * Post Deuce transformation for each class
				 */
				if(cv.exclude || postEnhancer == null){
					byteCodes.add(new ClassByteCode( cb.getClassName(), bytecode));
				}
				else{
					List<ClassByteCode> res = postEnhancer.visit(offline, cb.getClassName(), bytecode);
					byteCodes.addAll(res);
				}			
				if(offline) {
					byteCodes.add(fieldsHolder.getClassByteCode());
				}
			}
		}

		if( VERBOSE){
			try {
				verbose(byteCodes);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return byteCodes;
	}
	
	/**
	 * Reads the bytecode and calculate the frames, to support 1.5- code.
	 * 
	 * @param className class to manipulate 
	 * @param classfileBuffer original byte code
	 *  
	 * @return bytecode with frames
	 */
	private byte[] addFrames(String className, byte[] classfileBuffer) {

		try{
			FramesCodeVisitor frameCompute = new FramesCodeVisitor( className);
			return frameCompute.visit( classfileBuffer); // avoid adding frames to Java6
		}
		catch( FramesCodeVisitor.VersionException ex){
			return classfileBuffer;
		}
	}

	public static void premain(String agentArgs, Instrumentation inst) throws Exception{
		UnsafeHolder.getUnsafe();
		logger.fine("Starting Duece agent");
		inst.addTransformer(new Agent());
	}
	
	/**
	 * Used for offline instrumentation.
	 * @param args input jar & output jar
	 * e.g.: "C:\Java\jdk1.6.0_19\jre\lib\rt.jar" "C:\rt.jar"
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception{
		UnsafeHolder.getUnsafe();
		logger.fine("Starting Duece translator");
		
		// TODO check args
		Agent agent = new Agent();
		agent.transformJar(args[0], args[1]);
	}
	
	private void transformJar( String inFileNames, String outFilenames) throws IOException, IllegalClassFormatException {
		
		String[] inFileNamesArr = inFileNames.split(";");
		String[] outFilenamesArr = outFilenames.split(";");
		if(inFileNamesArr.length != outFilenamesArr.length)
			throw new IllegalArgumentException("Input files list length doesn't match output files list.");
		
		for(int i=0 ; i<inFileNamesArr.length ; ++i){
			String inFileName = inFileNamesArr[i];
			String outFilename = outFilenamesArr[i];
			
			final int size = 4096;
			byte[] buffer = new byte[size];
			ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
			JarInputStream jarIS = new JarInputStream(new FileInputStream(inFileName));
			JarOutputStream jarOS = new JarOutputStream(new FileOutputStream(outFilename), jarIS.getManifest());

			logger.info("Start translating source:" + inFileName + " target:" + outFilename);

			String nextName = "";
			try {
				for (JarEntry nextJarEntry = jarIS.getNextJarEntry(); nextJarEntry != null;
				nextJarEntry = jarIS.getNextJarEntry()) {

					baos.reset();
					int read;
					while ((read = jarIS.read(buffer, 0, size)) > 0) {
						baos.write(buffer, 0, read);
					}
					byte[] bytecode = baos.toByteArray();

					nextName = nextJarEntry.getName();
					if( nextName.endsWith(".class")){
						if( logger.isLoggable(Level.FINE)){
							logger.fine("Transalating " + nextName);
						}
						String className = nextName.substring(0, nextName.length() - ".class".length());
						List<ClassByteCode> transformBytecodes = transform( className, bytecode, true);
						for(ClassByteCode byteCode : transformBytecodes){
							JarEntry transformedEntry = new JarEntry(byteCode.getClassName() + ".class");
							jarOS.putNextEntry( transformedEntry); 
							jarOS.write( byteCode.getBytecode());
						}
					}
					else{
						jarOS.putNextEntry( nextJarEntry);
						jarOS.write(bytecode);
					}
				}

			}
			catch(Exception e){
				logger.log(Level.SEVERE, "Failed to translate " + nextName, e);
			}
			finally {
				logger.info("Closing source:" + inFileName + " target:" + outFilename);
				jarIS.close();
				jarOS.close();
			}
		}
	}
	
	private void verbose(List<ClassByteCode> byteCodes) throws FileNotFoundException,
	IOException {
		File verbose = new File( "verbose");
		verbose.mkdir();

		for( ClassByteCode byteCode : byteCodes){
			String[] packages = byteCode.getClassName().split("/");
			File file = verbose;
			for( int i=0 ; i<packages.length-1 ; ++i){
				file = new File( file, packages[i]);
				file.mkdir();
			}
			file = new File( file, packages[packages.length -1]);
			FileOutputStream fs = new FileOutputStream( file + ".class");
			fs.write(byteCode.getBytecode());
			fs.close();
		}
	}
}
