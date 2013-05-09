package org.deuce.transform.jvstm;

import org.deuce.objectweb.asm.AnnotationVisitor;
import org.deuce.objectweb.asm.ClassAdapter;
import org.deuce.objectweb.asm.ClassReader;
import org.deuce.objectweb.asm.ClassWriter;
import org.deuce.objectweb.asm.FieldVisitor;
import org.deuce.objectweb.asm.MethodVisitor;
import org.deuce.objectweb.asm.Opcodes;
import org.deuce.transform.Exclude;
import org.deuce.transform.asm.ClassByteCode;
import org.deuce.objectweb.asm.Type;

/**
 * Creates a new class with the suffix __FIELDS__ that extends a transactional class.
 * This new __FIELDS__ class is just instantiated by the replicate() method of its 
 * transactional base class.
 * This class just stores fields and all the inherited methods throw exception. 
 * Its purpose is just as a state repository.
 */
@Exclude
public class TransformerSubclassAom extends ClassAdapter implements Opcodes, JvstmConstants{

    public static ClassByteCode visit(String superClassName, byte[] superClassBytecodes){
	TransformerSubclassAom cv = new TransformerSubclassAom(superClassName, superClassBytecodes);
	return cv.visit();
    }
    private ClassByteCode visit(){
	ClassReader cr = new ClassReader(superClassBytecodes);
	cr.accept(this, 0);
	byte [] derivedClass = ((ClassWriter)super.cv).toByteArray();
	return new ClassByteCode(derivedClassName, derivedClass);
    }
    /*---------------------------------------------------------*
     *~~~~~~~~~~~~~~~          FIELDS      ~~~~~~~~~~~~~~~~~~~~*
     *---------------------------------------------------------*/    
    final String superClassName;
    final String derivedClassName;
    final byte[] superClassBytecodes;

    /*---------------------------------------------------------*
     *~~~~~~~~~~~~~~~       CONSTRUCTOR    ~~~~~~~~~~~~~~~~~~~~*
     *---------------------------------------------------------*/    

    public TransformerSubclassAom(String superClassName, byte[] superClassBytecodes){
	super(new ClassWriter(ClassWriter.COMPUTE_MAXS));
	this.superClassName = superClassName;
	this.derivedClassName = superClassName + SUFFIX_FIELDS;
	this.superClassBytecodes = superClassBytecodes;
    }
    /*---------------------------------------------------------*
     *~~~~~~~~~~~~~~  ClassVisitor INTERFACE  ~~~~~~~~~~~~~~~~~*
     *---------------------------------------------------------*/
    /**
     * Creates a new class with the same name of the superClass and the suffix __AOM__.
     * This new class extends the class with the name superClassName.
     */
    @Override
    public void visit(int version,int access,String name,String signature,String superName,String[] interfaces){
	super.visit(
		version, 
		access, 
		derivedClassName, 
		"L"+ superClassName +";", 
		superClassName, 
		null);
    }
    /**
     * Removes all the annotations inherited from the base class.
     */
    @Override
    public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
	return null;
    }
    /**
     * Removes all the fields already declared in the base class.
     */
    @Override
    public FieldVisitor visitField(int arg0, String arg1, String arg2, String arg3, Object arg4) {
	return null;
    }
    /**
     * Ignore all constructors, static and private methods - non virtual methods
     * Overrides every instance method and throws UnsupportedOperationException.
     */
    @Override
    public MethodVisitor visitMethod(int access,String name,String desc,String signature,String[] exceptions) {
	if(name.equals("<clinit>") || name.equals("<init>") || ((access & (ACC_STATIC | ACC_PRIVATE)) != 0)){
	    return null;
	}
	access &= (~ACC_FINAL); // Removes final if exists,
	MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
	mv.visitTypeInsn(NEW, "java/lang/UnsupportedOperationException");
	mv.visitInsn(DUP);
	mv.visitMethodInsn(INVOKESPECIAL, "java/lang/UnsupportedOperationException", "<init>", "()V");
	mv.visitInsn(ATHROW);
	mv.visitMaxs(2, 1);
	return null;
    }
    /**
     * Adds a static field OBJECT_SIZE with the size of the instances of the 
     * corresponding transactional class (without __FIELDS__ suffix).
     */
    @Override
    public void visitEnd() {
	addObjectSizeField();
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
	//
	// OBJECT_SIZE initialization
	//
	MethodVisitor mv = super.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
	mv.visitCode();
	mv.visitLdcInsn(Type.getType("L" + superClassName + ";"));
	mv.visitMethodInsn(INVOKESTATIC, VBOX_OFFSETS_INTERNAL, "sizeOf", "(Ljava/lang/Class;)J");
	mv.visitFieldInsn(PUTSTATIC, derivedClassName , FIELD_NAME_OBJECT_SIZE, "J");
	mv.visitInsn(RETURN);
	mv.visitMaxs(1, 0);
	mv.visitEnd();
	
    }
}
