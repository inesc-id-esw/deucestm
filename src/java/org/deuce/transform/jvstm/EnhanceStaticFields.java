package org.deuce.transform.jvstm;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.deuce.objectweb.asm.AnnotationVisitor;
import org.deuce.objectweb.asm.ClassAdapter;
import org.deuce.objectweb.asm.ClassReader;
import org.deuce.objectweb.asm.ClassWriter;
import org.deuce.objectweb.asm.FieldVisitor;
import org.deuce.objectweb.asm.MethodAdapter;
import org.deuce.objectweb.asm.MethodVisitor;
import org.deuce.objectweb.asm.Opcodes;
import org.deuce.objectweb.asm.Type;
import org.deuce.objectweb.asm.tree.FieldNode;
import org.deuce.transform.Exclude;
import org.deuce.transform.asm.ClassByteCode;
import org.deuce.transform.asm.ClassEnhancer;
import org.deuce.transform.asm.ExcludeIncludeStore;

/**
 * This transformation will replace all accesses to static fields of transactional classes
 * with an equivalent instance field access to a singleton object that replaces
 * the object Class holder for common static fields. 
 * If this class is also transactional (i.e. Not Excluded by Deuce and included by AomStorage)
 * then it will remove all its static fields and put them as instance fields into a new class 
 * with the same name and the __STATICFIELDS__ suffix. 
 * 
 * @author mcarvalho
 */
@Exclude
public class EnhanceStaticFields extends ClassAdapter implements ClassEnhancer, Opcodes, JvstmConstants{
    
    /**
     * Utility function to produce an auxiliary class containing the 
     * instance fields corresponding to the static fields of the class 
     * being instrumented by this enhancer.  
     */
    private static ClassByteCode generateStaticFieldsClass(List<FieldInfo> staticFields, String className){
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_6, ACC_PUBLIC | ACC_FINAL, className, "Ljava/lang/Object;", "java/lang/Object", null);
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        for (FieldInfo f : staticFields) {
            int access = f.access & ~ACC_STATIC; // Remove static modifier
            access &= ~ACC_PRIVATE; // Remove private modifier
            access &= ~ACC_PROTECTED; // Remove protected modifier
            access &= ~ACC_FINAL; // Remove final modifier // ????? VERIFICAR SE o MESMO e' necessario
            access |= ACC_PUBLIC; // Add public modifier
            FieldNode fn = new FieldNode(access, f.name, f.desc, f.signature, f.value);
            fn.accept(cw);
        }
        cw.visitEnd();
        return new ClassByteCode(className, cw.toByteArray());
    }

    /*---------------------------------------------------------*
     *~~~~~~~~~~~~~~~   AUXILIARY CONSTANTS  ~~~~~~~~~~~~~~~~~~*
     *---------------------------------------------------------*/    

    final static public String EXCLUDE_DESC = Type.getDescriptor(Exclude.class);
    final static private String ANNOTATION_NAME = Type.getInternalName(Annotation.class);
    
    final static private Collection<String> excludeClasses = Arrays.asList(
	    "java/util/logging/LoggingProxyImpl", 
	    "java/util/logging/Logger",
	    "java/util/logging/Level",
	    "java/util/concurrent/TimeUnit");
    
        
    /*---------------------------------------------------------*
     *~~~~~~~~~~~~~~~          FIELDS      ~~~~~~~~~~~~~~~~~~~~*
     *---------------------------------------------------------*/    
    String className;
    byte[] classBytecodes;
    final List<FieldInfo> staticFields;
    boolean staticFieldsAlreadyInitialized = false;
    
    @Exclude
    static class FieldInfo{
	final int access;
	final String name; 
	final String desc; 
	final String signature; 
	final Object value;
	public FieldInfo(int access, String name, String desc, String signature, Object value) {
	    super();
	    this.access = access;
	    this.name = name;
	    this.desc = desc;
	    this.signature = signature;
	    this.value = value;
	}
    }
    
    /*---------------------------------------------------------*
     *~~~~~~~~~~~~~~~       CONSTRUCTOR    ~~~~~~~~~~~~~~~~~~~~*
     *---------------------------------------------------------*/

    /**
     * The visit method from the ClassEnhancer interface implementation 
     * will create a new instance applying this constructor.      
     */    
    public EnhanceStaticFields() {
	super(new ClassWriter(0));
	this.staticFields = new LinkedList<FieldInfo>();
    }
        
    /*---------------------------------------------------------*
     *~~~~~~~~~~~~~  Interface ClassEnhancer ~~~~~~~~~~~~~~~~~~*
     *---------------------------------------------------------*/    

    @Override
    public List<ClassByteCode> visit(boolean offline, String className, byte[] classfileBuffer) {
        List<ClassByteCode> res = new LinkedList<ClassByteCode>();
        this.className = className;
        this.classBytecodes = classfileBuffer;
        ClassReader cr = new ClassReader(classBytecodes);
        try{
            cr.accept(this, 0); // this call will dispatch the invocation to the visit method bellow
            byte[] transformedClass = ((ClassWriter)super.cv).toByteArray();
            res.add(new ClassByteCode(className, transformedClass));// Adds the transformed class which replaces getstatic/putstatic by getfiedl/putfield  from a __STATICFIELDS__ class
            if(!this.staticFields.isEmpty()){
                res.add(generateStaticFieldsClass(this.staticFields, className + SUFFIX_STATICFIELDS));
            }
        }catch(StopInstrumentationException e){
            // That's because the transformation process has been interrupted 
            // due to the source class being an annotated with Exclude.
            // In this case return the original untransformed type.
            res.add(new ClassByteCode(className, classfileBuffer));
        }
        return res;
    }

    /*---------------------------------------------------------*
     *~~~~~~~~~~~~~~  ClassVisitor INTERFACE  ~~~~~~~~~~~~~~~~~*
     *---------------------------------------------------------*/
    
    /**
     * Checks if this class is an annotation.
     */
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        for(String inter : interfaces){
            if( inter.equals(ANNOTATION_NAME)){
                throw new StopInstrumentationException();
            }
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }
    
    /**
     * Checks if the class is marked as {@link Exclude @Exclude}
     */
    @Override
    public AnnotationVisitor visitAnnotation( String desc, boolean visible) {
            if(EXCLUDE_DESC.equals(desc))
                throw new StopInstrumentationException();
            return super.visitAnnotation(desc, visible);
    }

    /**
     * Identifies all static fields that will be moved into the __STATICFIELDS__
     * class as instance fields.
     * For now we will also move the final static fields because there is no easy way 
     * of detecting final fields in the visitFieldInsn method - its descriptor has no 
     * information about access modifiers - and we will access them in the __STATIC_PART$__
     * of this object.
     */
    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
	if(ExcludeIncludeStore.exclude(className) // excluded by Deuce
		|| ExcludeIncludeStore.immutable(className)
		|| name.contains("$") // excluded by Deuce
		|| excludeClasses.contains(className) // excluded by this feature
		|| !JvstmStorage.checkTransactional(className) // this is not a transactional class
		||((access & ACC_STATIC) == 0) // not a static field
	     // || ((access & ACC_FINAL) != 0)) // for now include FINAL fields too.
	)
	{
	    return super.visitField(access, name, desc, signature, value);
	}
	staticFields.add(new FieldInfo(access, name, desc, signature, value));
	return null;	

    }
    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
	MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
	
	if(name.equals("<clinit>") && !staticFields.isEmpty()){
	    staticFieldsAlreadyInitialized = true;
	    mv = new MethodAdapter(mv) {
		@Override
		public void visitCode() {
		    initStaticFields(mv);
		    super.visitCode();
		}
		@Override
		public void visitMaxs(int maxStack, int maxLocals){
		    super.visitMaxs(maxStack + 2, maxLocals);
		}
	    };
	}
	
	return new MethodAdapter(mv) {
	    private boolean makeReadBarrier = false, makeWriteBarrier = false; 
	    
	    @Override
	    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		if(ExcludeIncludeStore.exclude(owner) // excluded by Deuce
			|| ExcludeIncludeStore.immutable(owner)
			|| name.contains("$") // excluded by Deuce
			|| excludeClasses.contains(owner) // excluded by this feature
			|| !JvstmStorage.checkTransactional(owner) // this is not a transactional class
		)
		{
		    super.visitFieldInsn(opcode, owner, name, desc);
		    return;
		}
		
		if((opcode == GETSTATIC)){
		    super.visitFieldInsn( // get static field STATIC_PART$ from the owner class 
			    GETSTATIC, 
			    owner, 
			    FIELD_NAME_STATIC_PART, 
			    "L" + owner + SUFFIX_STATICFIELDS + ";");
		    super.visitFieldInsn( // get field name from the owner__STATICFIELDS__ class
			    GETFIELD, 
			    owner + SUFFIX_STATICFIELDS , 
			    name, 
			    desc);
		    makeReadBarrier = true;
		    return;
		}
		if((opcode == PUTSTATIC)){
		    super.visitFieldInsn( // get static field STATIC_PART$ from the owner class 
			    GETSTATIC, // 178 
			    owner, 
			    FIELD_NAME_STATIC_PART, 
			    "L" + owner + SUFFIX_STATICFIELDS + ";");
		    if(desc.equals("J") || desc.equals("D") ){
			super.visitInsn(Opcodes.DUP_X2); // 91
			super.visitInsn(Opcodes.POP); // 87
			super.visitFieldInsn( // put field name from the owner__STATICFIELDS__ class
				PUTFIELD, // 181 
				owner + SUFFIX_STATICFIELDS , 
				name, 
				desc);
	                return;
		    }

		    super.visitInsn(SWAP);
		    super.visitFieldInsn( // put field name from the owner__STATICFIELDS__ class
			    PUTFIELD, 
			    owner + SUFFIX_STATICFIELDS , 
			    name, 
			    desc);
		    makeWriteBarrier = true;
		    return;
		}
	        super.visitFieldInsn(opcode, owner, name, desc);
	    }
	    
	    @Override
	    public void visitMaxs(int maxStack, int maxLocals){
	        if(makeWriteBarrier) maxStack += 6;
	        else if(makeReadBarrier) maxStack += 4;
	        super.visitMaxs(maxStack, maxLocals);
	    }

	};
    }
    @Override
    public void visitEnd() {
	if(!staticFields.isEmpty()){
	    //
	    // Static fields declaration
	    //
	    FieldVisitor fv = super.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, FIELD_NAME_STATIC_PART, "L" + className + SUFFIX_STATICFIELDS + ";", null, null);
	    fv.visitEnd();
	    //
	    // Static constructor
	    //
	    if(!staticFieldsAlreadyInitialized){
		MethodVisitor mv = super.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
		mv.visitCode();
		initStaticFields(mv);
		mv.visitInsn(RETURN);
		mv.visitMaxs(2, 0);
		mv.visitEnd();
	    }
	}
    }	
    private void initStaticFields(MethodVisitor mv){
	mv.visitTypeInsn(NEW, className + SUFFIX_STATICFIELDS);
	mv.visitInsn(DUP);
	mv.visitMethodInsn(INVOKESPECIAL, className + SUFFIX_STATICFIELDS, "<init>", "()V");
	mv.visitFieldInsn(PUTSTATIC, className, FIELD_NAME_STATIC_PART, "L" + className + SUFFIX_STATICFIELDS + ";");
    }
}
