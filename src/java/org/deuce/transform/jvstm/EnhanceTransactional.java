package org.deuce.transform.jvstm;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


import java.util.Collection;

import jvstm.Transaction;

import org.deuce.objectweb.asm.ClassAdapter;
import org.deuce.objectweb.asm.ClassReader;
import org.deuce.objectweb.asm.ClassWriter;
import org.deuce.objectweb.asm.FieldVisitor;
import org.deuce.objectweb.asm.Label;
import org.deuce.objectweb.asm.MethodAdapter;
import org.deuce.objectweb.asm.MethodVisitor;
import org.deuce.objectweb.asm.Opcodes;
import org.deuce.objectweb.asm.Type;
import org.deuce.transaction.Context;
import org.deuce.transform.Exclude;
import org.deuce.transform.asm.ClassByteCode;
import org.deuce.transform.asm.ClassEnhancer;
import org.deuce.transform.jvstm.vboxes.VBoxObjectArray;

/**
 * This class provides all the required transformations to use the JVSTM in the Deuce.
 * The JVSTM requires that all transactional objects (i.e. objects accessed inside a 
 * transactional scope) store metadata - versioned history.
 * For this purpose we will change the top of the class hierarchy from Object to the
 * VBoxAom class - whose instances are the head of the versioned history of transactional 
 * objects.
 * This class is a ClassEnhancer that performs a pos transformation and that should be specified
 * to the Deuce engine via org.deuce.transform.pos system property:
 *  e.g. -Dorg.deuce.transform.pos=org.deuce.transform.jvstm.EnhanceTransactional
 *  
 * @author mcarvalho
 */
@Exclude
public class EnhanceTransactional extends ClassAdapter implements ClassEnhancer, Opcodes, JvstmConstants{
    
    /*===========================================================================*
     *~~~~~~~~~~~~~~~~~~~~~~~~~~      IMMUTABLES      ~~~~~~~~~~~~~~~~~~~~~~~~~~~*
     *===========================================================================*/

    /**
     * These classes should not be transformed even when they are considered transactional
     * candidates by the JvstmStorage.
     */
    private static final Collection<String> EXCLUDE_CLASSES = Arrays.asList(
	    "java/lang/Enum", "java/lang/Thread", "java/lang/ThreadLocal",
	    "java/lang/Exception", "java/lang/RuntimeException", "java/lang/Throwable");

    final static private int JAVA6_VERSION = 50;
    
    /*===========================================================================*
     *~~~~~~~~~~~~~~~~~~~~~~~~~~       FIELDS         ~~~~~~~~~~~~~~~~~~~~~~~~~~~*
     *===========================================================================*/

    String className;
    String derivedClassName;
    byte[] classBytecodes;
    boolean offline;
    boolean objectFieldSizeInitialized;
    private boolean interceptConstructors;
    private boolean isAbstractClass;
    private int version;

    /*===========================================================================*
     *~~~~~~~~~~~~~~~~~~~~~~~~~~       CONSTRUCTOR    ~~~~~~~~~~~~~~~~~~~~~~~~~~~*
     *===========================================================================*/

    public EnhanceTransactional() {
	super(new ClassWriter(0));	
    }
    
    /*===========================================================================*
     *~~~~~~~~~~~~~~~~~~~~~~~~~~       PROPERTIES     ~~~~~~~~~~~~~~~~~~~~~~~~~~~*
     *===========================================================================*/
    
    public boolean isAbstractClass() {
	return isAbstractClass;
    }
    
    /*===========================================================================*
     *~~~~~~~~~~~~~~~~~~~~~~~    ClassEnhancer Methods      ~~~~~~~~~~~~~~~~~~~~~*
     *===========================================================================*/

    /**
     * If the class corresponding to the classfileBuffer received by argument is not Transactional,
     * then it return a new List just containing this unmodified class.
     * We considerer all non abstract types as Transactional classes.
     * Otherwise, for transactional classes, it will enhance it with the AOM methods and will change 
     * its top base class from Object to the VBoxAom class.
     * Then it will also implement the inherited abstract methods such as the toCompactLayout()
     * and replicate().
     * In this case it will return a new List containing the enhanced class and a new __FIELDS__ class that 
     * extends the previous one. This __FIELDS__ class pretends to be just a fields storage where all
     * inherited methods throw exception.
     */
    public List<ClassByteCode> visit(boolean offline, String className, byte[] classfileBuffer) {
        List<ClassByteCode> res = new LinkedList<ClassByteCode>();
        if(!JvstmStorage.checkTransactional(className)){
            res.add(new ClassByteCode(className, classfileBuffer));
        }else{
            this.derivedClassName = className;
            if(offline) this.derivedClassName += SUFFIX_FIELDS;
            this.offline = offline;
            this.className = className;
            this.classBytecodes = classfileBuffer;
            this.interceptConstructors = false;
            this.isAbstractClass = false;
            this.objectFieldSizeInitialized = false;
            
            try{
                ClassReader cr = new ClassReader(classBytecodes);
                cr.accept(this, 0); // this call will dispatch the invocation to the visit method bellow
                byte[] transformedClass = ((ClassWriter)super.cv).toByteArray();
                res.add(new ClassByteCode(className, transformedClass)); // Adds the transformed class, enhanced with the AOM infrastructure.
                if(!isAbstractClass() && offline)
                    res.add(TransformerSubclassAom.visit(className, classfileBuffer)); // Adds the derived class that will be just a repository of fields.
            }catch(StopInstrumentationException e){
                // That's because the transformation process has been interrupted 
                // due to the source class being an interface.
                // In this case return the original untransformed type.
                res.add(new ClassByteCode(className, classfileBuffer));
            }
        }
        return res;
    }
    
    /*===========================================================================*
     *~~~~~~~~~~~~~~~~~~~~~~~~~~ ClassVisitor METHODS ~~~~~~~~~~~~~~~~~~~~~~~~~~~*
     *===========================================================================*/
    
    /**
     * If this class is not an interface and it inherits from Object, then it will 
     * replace the base class by VBoxAom.
     */
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces){
        this.version = version;
        
	if((access & ACC_INTERFACE) != 0 ){
	    throw new StopInstrumentationException();   
	}
	if(EXCLUDE_CLASSES.contains(superName)){
	    throw new StopInstrumentationException();
	}
	if((access & ACC_ABSTRACT) != 0 ){
	    isAbstractClass= true;
	}
	if(superName.equals(OBJECT_INTERNAL)){
	    interceptConstructors = true;
	    superName = VBOX_INTERNAL;
	    if(signature == null){
		signature = JvstmUtils.parametrizeVBoxDesc(className);
		for (String interf : interfaces) {
		    signature += "L" + interf + ";";
		}
	    }else{
		String typeParams = null;
		// If this class starts with < then it declares 
		// type parameters.
		if(signature.indexOf("<") == 0){
		    int idx = signature.indexOf(">");
		    typeParams = signature.substring(0, idx + 1);
		    signature = signature.substring(idx + 1);
		}
		if(signature.indexOf(OBJECT_DESC) == 0){
		    signature = signature.replace("Ljava/lang/Object;", JvstmUtils.parametrizeVBoxDesc(className));
		}
		if(typeParams != null) 
		    signature = typeParams + signature;
	    }
	}
	super.visit(
		version, 
		access & (~ACC_FINAL), // Removes final if exists,
		name, 
		signature, 
		superName, 
		interfaces);
    }
    
    /**
     * Changes the invocation of the super constructor from Object to VBoxAom class.
     * Removes final for all methods if exists.
     * If the method intercepted here is the class constructor and in case of offline instrumentation, 
     * then it will initialize the OBJECT_SIZE field.
     */
    @Override
    public MethodVisitor visitMethod(int access,final String methodName,final String desc,String signature,String[] exceptions){
	access &= ~ACC_FINAL;
	final MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);

	if(!offline && methodName.equals("<clinit>")){
	    objectFieldSizeInitialized = true;
	    return new MethodAdapter(mv) {
		@Override
		public void visitCode() {
		    initObjectSizeField(mv);
		    super.visitCode();
		}
		@Override
		public void visitMaxs(int maxStack, int maxLocals){
		    super.visitMaxs(maxStack + 2, maxLocals);
		}
	    };
	}else if(interceptConstructors && methodName.equals("<init>")){
	    final int argumentsSize = DescUtil.ctorArgumentsSize(desc, access);
	    return new MethodAdapter(mv) {
		boolean firstCallToSuper = true;
		@Override
		public void visitMethodInsn(int opcode, String owner,String name,String desc) {
		    if(firstCallToSuper && interceptConstructors && methodName.equals("<init>") && opcode == INVOKESPECIAL && owner.equals("java/lang/Object")){
			// Changes the invocation of the super constructor from Object to VBoxAom class.
			// Just do this for the first invocation corresponding to the super() call.
			// Following uses of the invokespecial to Object constructor could be for 
			// new instances of Object, that should not be replaced by VBoxAom call.
			// !! Eventual misbehaviour => it can instruments the wrong invokespecial call. 
			// !! e.g. BUG: super(..., new Object()) - in this case instead of the first call 
			// to Object invokespecial we should instrument the second one. 
			// !! For now, we will keep it simple like this as a minor workaround !!
			//
			firstCallToSuper = false;

			// Changes the invocation of the super constructor from Object to VBoxAom class
			owner = VBOX_INTERNAL; 
			if(argumentsSize >= 0){ 
                            // It returns a negative value if this constructor 
                            // does not contain a Context argument.
                            super.visitVarInsn(ALOAD, argumentsSize - 1); // load context
                            super.visitTypeInsn( CHECKCAST, CLASS_JVSTM_CTX);
                            super.visitFieldInsn(GETFIELD, CLASS_JVSTM_CTX, FIELD_NAME_CURRENT_TRX, CLASS_JVSTM_TRX_DESC); // get current Transsaction from the context                            
                            desc = "(" + CLASS_JVSTM_TRX_DESC + ")V";
                        }
		    }
		    super.visitMethodInsn(opcode, owner, name, desc);
		}
		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
		    if(argumentsSize >= 0){ 
			maxStack++;
		    }
		    super.visitMaxs(maxStack, maxLocals);
		}
	    };
	}else{
	    return mv;
	}
    }
    
    /**
     * If this class is nor abstract neither an interface then adds the implementation 
     * of the replicate method from the interface Replicable
     */
    @Override
    public void visitEnd() {
	//
	// Implements the unimplemented methods of the VBoxAom class: replicate() toCompactLayout()
	//
	if(!isAbstractClass) 
	    methodsOfAOM();
	//
	// If it is not in offline mode then we add to this class the OBJECT_SIZE field. 
	//
	if(!offline){
	    addObjectSizeField();
	    if(!objectFieldSizeInitialized){
		MethodVisitor mv = super.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
		initObjectSizeField(mv);
		mv.visitInsn(RETURN);
		mv.visitMaxs(2, 0);
		mv.visitEnd();
	    }
	}
	super.visitEnd();
    }
    /**
     * Adds a static field OBJECT_SIZE with the size of the instances of the 
     * corresponding transactional class (without __FIELDS__ suffix).
     */
    public void addObjectSizeField(){
	//
	// Declaration of the static field OBJECT_SIZE.
	//
	FieldVisitor fv = super.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, FIELD_NAME_OBJECT_SIZE , "J", null, null);
	fv.visitEnd();
    }
    public void initObjectSizeField(MethodVisitor mv ){
	//
	// OBJECT_SIZE initialization
	//
	mv.visitCode();
	mv.visitLdcInsn(Type.getType("L" + className + ";"));
	mv.visitMethodInsn(INVOKESTATIC, VBOX_OFFSETS_INTERNAL, "sizeOf", "(Ljava/lang/Class;)J");
	mv.visitFieldInsn(PUTSTATIC, derivedClassName , FIELD_NAME_OBJECT_SIZE, "J");
    }
    /**
     * Implementation of the AOM (adaptive object metadata) methods specified in the
     * VBoxAom class.
     */
    private void methodsOfAOM(){
	//
	// Replicate method with generic parameter 
	// 
	{
	    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "replicate", "()L"+ className +";", null, null);
	    mv.visitCode();
	    //
	    // Initialize try block
	    //
	    Label l0 = new Label();
	    Label l1 = new Label();
	    Label l2 = new Label();
	    mv.visitTryCatchBlock(l0, l1, l2, "java/lang/InstantiationException");
	    mv.visitLabel(l0);
	    //
	    // Creates the replica - instance of the class with the same name and the __FIELDS__ suffix
	    //
	    mv.visitFieldInsn(GETSTATIC, "jvstm/UtilUnsafe", "UNSAFE", "Lsun/misc/Unsafe;");
	    mv.visitLdcInsn(Type.getType("L" + derivedClassName + ";"));
	    mv.visitMethodInsn(INVOKEVIRTUAL, "sun/misc/Unsafe", "allocateInstance", "(Ljava/lang/Class;)Ljava/lang/Object;");
	    mv.visitTypeInsn(CHECKCAST, className);
	    mv.visitVarInsn(ASTORE, 1);
	    //
	    // The header object has 12 bytes and the first field starts at offset 12. 
	    // Yet, we do not want to copy the first two fields corresponding to the fields
	    // inherited from the VBoxAom class: body and inplace. 
	    // So we will start copying from the offset 20 henceforward.
	    // First we will copy one word of 4 bytes and after that we will copy words of 8 bytes,
	    // because we are running in a 64 bits architecture and all objects are multiples of 8 bytes.
	    //
	    /* 1st arg of putLong = Unsafe object     */ mv.visitFieldInsn(GETSTATIC, "jvstm/UtilUnsafe", "UNSAFE", "Lsun/misc/Unsafe;");
	    /* 2nd arg of putLong = className object  */ mv.visitVarInsn(ALOAD, 1);
	    /* 3rd arg of putLong = offset            */ mv.visitLdcInsn(20L);
	    /* 1st arg of getLong = Unsafe object     */ mv.visitFieldInsn(GETSTATIC, "jvstm/UtilUnsafe", "UNSAFE", "Lsun/misc/Unsafe;");
	    /* 2nd arg of getLong = this              */ mv.visitVarInsn(ALOAD, 0);
	    /* 3rd arg of getLong = offset            */ mv.visitLdcInsn(20L);
	    /* 4th arg of putLong = result of getLong */ mv.visitMethodInsn(INVOKEVIRTUAL, "sun/misc/Unsafe", "getInt", "(Ljava/lang/Object;J)I");
	    /*                                        */ mv.visitMethodInsn(INVOKEVIRTUAL, "sun/misc/Unsafe", "putInt", "(Ljava/lang/Object;JI)V");
	    //
	    // Performs the loop copying word by word.
	    // 
	    mv.visitLdcInsn(new Long(24L));
	    mv.visitVarInsn(LSTORE, 3);
	    Label l3 = new Label();
	    mv.visitLabel(l3);
	    if(version >= JAVA6_VERSION) mv.visitFrame(Opcodes.F_APPEND,3, new Object[] {className, INTEGER, LONG}, 0, null);
	    mv.visitVarInsn(LLOAD, 3);
	    mv.visitFieldInsn(GETSTATIC, derivedClassName, FIELD_NAME_OBJECT_SIZE, "J");
	    mv.visitInsn(LCMP);
	    Label l4 = new Label();
	    mv.visitJumpInsn(IFGE, l4);
	    mv.visitFieldInsn(GETSTATIC, "jvstm/UtilUnsafe", "UNSAFE", "Lsun/misc/Unsafe;");
	    mv.visitVarInsn(ALOAD, 0);
	    mv.visitVarInsn(LLOAD, 3);
	    mv.visitMethodInsn(INVOKEVIRTUAL, "sun/misc/Unsafe", "getLong", "(Ljava/lang/Object;J)J");
	    mv.visitVarInsn(LSTORE, 5);
	    mv.visitFieldInsn(GETSTATIC, "jvstm/UtilUnsafe", "UNSAFE", "Lsun/misc/Unsafe;");
	    mv.visitVarInsn(ALOAD, 1);
	    mv.visitVarInsn(LLOAD, 3);
	    mv.visitVarInsn(LLOAD, 5);
	    mv.visitMethodInsn(INVOKEVIRTUAL, "sun/misc/Unsafe", "putLong", "(Ljava/lang/Object;JJ)V");
	    mv.visitVarInsn(LLOAD, 3);
	    mv.visitLdcInsn(new Long(8L));
	    mv.visitInsn(LADD);
	    mv.visitVarInsn(LSTORE, 3);
	    mv.visitJumpInsn(GOTO, l3);
	    mv.visitLabel(l4);
	    if(version >= JAVA6_VERSION) mv.visitFrame(Opcodes.F_CHOP,1, null, 0, null);
	    mv.visitVarInsn(ALOAD, 1);
	    mv.visitLabel(l1);
	    mv.visitInsn(ARETURN);
	    //
	    // The catch block
	    // 
	    mv.visitLabel(l2);
	    if(version >= JAVA6_VERSION) mv.visitFrame(Opcodes.F_FULL, 1, new Object[] {className}, 1, new Object[] {"java/lang/InstantiationException"});
	    mv.visitVarInsn(ASTORE, 1);
	    mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
	    mv.visitInsn(DUP);
	    mv.visitVarInsn(ALOAD, 1);
	    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V");
	    mv.visitInsn(ATHROW);
	    mv.visitMaxs(9, 7);
	    mv.visitEnd();
	}
	//
	// Replicate method overloaded for VBoxAom
	// 
	MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC, "replicate", "()" + OBJECT_DESC, null, null);
	mv.visitCode();
	mv.visitVarInsn(ALOAD, 0);
	mv.visitMethodInsn(INVOKEVIRTUAL, className, "replicate", "()L" + className + ";");
	mv.visitInsn(ARETURN);
	mv.visitMaxs(1, 1);
	mv.visitEnd();
	//
	// toCompactLayout method with generic parameter  
	// 
	{
	    //
	    // The header object has 12 bytes and the first field starts at offset 12.
	    // Yet, we do not want to copy the first two fields corresponding to the fields
	    // inherited from the VBoxAom class: body and inplace. 
	    // So we will start copying from the offset 20 henceforward.
	    // First we will copy one word of 4 bytes and after that we will copy words of 8 bytes,
	    // because we are running in a 64 bits architecture and all objects are multiples of 8 bytes.
	    //
	    mv = super.visitMethod(ACC_PUBLIC, "toCompactLayout", "(L" +  className + ";)V", null, null);
	    mv.visitCode();
	    mv.visitFieldInsn(GETSTATIC, "jvstm/UtilUnsafe", "UNSAFE", "Lsun/misc/Unsafe;");
	    mv.visitVarInsn(ALOAD, 1);
	    mv.visitLdcInsn(new Long(20L));
	    mv.visitMethodInsn(INVOKEVIRTUAL, "sun/misc/Unsafe", "getInt", "(Ljava/lang/Object;J)I");
	    mv.visitVarInsn(ISTORE, 2);
	    mv.visitFieldInsn(GETSTATIC, "jvstm/UtilUnsafe", "UNSAFE", "Lsun/misc/Unsafe;");
	    mv.visitVarInsn(ALOAD, 0);
	    mv.visitLdcInsn(new Long(20L));
	    mv.visitVarInsn(ILOAD, 2);
	    mv.visitMethodInsn(INVOKEVIRTUAL, "sun/misc/Unsafe", "putInt", "(Ljava/lang/Object;JI)V");
	    //
	    // Performs the loop copying word by word.
	    // 
	    mv.visitLdcInsn(new Long(24L));
	    mv.visitVarInsn(LSTORE, 3);
	    Label l0 = new Label();
	    mv.visitLabel(l0);
	    if(version >= JAVA6_VERSION) mv.visitFrame(Opcodes.F_APPEND,2, new Object[] {Opcodes.INTEGER, Opcodes.LONG}, 0, null);
	    mv.visitVarInsn(LLOAD, 3);
	    mv.visitFieldInsn(GETSTATIC, derivedClassName, FIELD_NAME_OBJECT_SIZE, "J");
	    mv.visitInsn(LCMP);
	    Label l1 = new Label();
	    mv.visitJumpInsn(IFGE, l1);
	    mv.visitFieldInsn(GETSTATIC, "jvstm/UtilUnsafe", "UNSAFE", "Lsun/misc/Unsafe;");
	    mv.visitVarInsn(ALOAD, 1);
	    mv.visitVarInsn(LLOAD, 3);
	    mv.visitMethodInsn(INVOKEVIRTUAL, "sun/misc/Unsafe", "getLong", "(Ljava/lang/Object;J)J");
	    mv.visitVarInsn(LSTORE, 5);
	    mv.visitFieldInsn(GETSTATIC, "jvstm/UtilUnsafe", "UNSAFE", "Lsun/misc/Unsafe;");
	    mv.visitVarInsn(ALOAD, 0);
	    mv.visitVarInsn(LLOAD, 3);
	    mv.visitVarInsn(LLOAD, 5);
	    mv.visitMethodInsn(INVOKEVIRTUAL, "sun/misc/Unsafe", "putLong", "(Ljava/lang/Object;JJ)V");
	    mv.visitVarInsn(LLOAD, 3);
	    mv.visitLdcInsn(new Long(8L));
	    mv.visitInsn(LADD);
	    mv.visitVarInsn(LSTORE, 3);
	    mv.visitJumpInsn(GOTO, l0);
	    mv.visitLabel(l1);
	    if(version >= JAVA6_VERSION) mv.visitFrame(Opcodes.F_CHOP,1, null, 0, null);
	    mv.visitInsn(RETURN);
	    mv.visitMaxs(6, 7);
	    mv.visitEnd();
	}
	//
	// toCompactLayout method overloaded for VBoxAom
	// 
	mv = super.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC, "toCompactLayout", "(" + OBJECT_DESC + ")V", null, null);
	mv.visitCode();
	mv.visitVarInsn(ALOAD, 0);
	mv.visitVarInsn(ALOAD, 1);
	mv.visitTypeInsn(CHECKCAST, className );
	mv.visitMethodInsn(INVOKEVIRTUAL, className, "toCompactLayout", "(L" + className+ ";)V");
	mv.visitInsn(RETURN);
	mv.visitMaxs(2, 2);
	mv.visitEnd();
    }
}
