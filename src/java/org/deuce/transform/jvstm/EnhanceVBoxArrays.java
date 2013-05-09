package org.deuce.transform.jvstm;

import java.io.IOError;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import jvstm.VBoxBody;

import org.deuce.Atomic;
import org.deuce.Irrevocable;
import org.deuce.objectweb.asm.AnnotationVisitor;
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
import org.deuce.transaction.ContextDelegator;
import org.deuce.transform.Exclude;
import org.deuce.transform.asm.ClassByteCode;
import org.deuce.transform.asm.ClassEnhancer;
import org.deuce.transform.asm.ExcludeIncludeStore;
import org.deuce.transform.asm.type.TypeCodeResolver;
import org.deuce.transform.asm.type.TypeCodeResolverFactory;
import org.deuce.transform.jvstm.vboxes.AbstractVBoxArray;
import org.deuce.transform.jvstm.vboxes.VBoxByteArray;
import org.deuce.transform.jvstm.vboxes.VBoxCharArray;
import org.deuce.transform.jvstm.vboxes.VBoxDoubleArray;
import org.deuce.transform.jvstm.vboxes.VBoxFloatArray;
import org.deuce.transform.jvstm.vboxes.VBoxIntArray;
import org.deuce.transform.jvstm.vboxes.VBoxLongArray;
import org.deuce.transform.jvstm.vboxes.VBoxObjectArray;
import org.deuce.transform.jvstm.vboxes.VBoxShortArray;

/*
 * Enhancement for wrapping arrays in VBoxArrays.
 */
@Exclude
public class EnhanceVBoxArrays extends ClassAdapter implements ClassEnhancer, Opcodes, JvstmConstants{

    @Exclude
    private static class MethodInfo{
	final int access;
	final String methodName;
	final String desc;
	final String signature;
	final String[] exceptions;
	public MethodInfo(int access, String methodName, String desc, String signature, String[] exceptions) {
	    super();
	    this.access = access;
	    this.methodName = methodName;
	    this.desc = desc;
	    this.signature = signature;
	    this.exceptions = exceptions;
	}
    }
    
    @Exclude
    private static class MethodTarget{
        final int opcode; 
        final String owner;
        final String name;
        final String desc;
        final String newDesc;
        public MethodTarget(int opcode, String owner, String name, String desc, String newDesc) {
            super();
            this.opcode = opcode;
            this.owner = owner;
            this.name = name;
            this.desc = desc;
            this.newDesc = newDesc;
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((desc == null) ? 0 : desc.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            MethodTarget other = (MethodTarget) obj;
            if (desc == null) {
                if (other.desc != null)
                    return false;
            } else if (!desc.equals(other.desc))
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            return true;
        }
        
    }
    
    /**
     * Returns the name of the method in this class that unwraps arrays from 
     * VBoxArrays and invokes the destination method.
     */
    private static String mediatorMethodName(String owner, String methodName){
        owner = owner.replace('/', '$');
        methodName = methodName.replace('<', '_'); 
        methodName = methodName.replace('>', '_');
        return owner + '$' + methodName;
    }
    
    /*---------------------------------------------------------*/
    /*---------------------------------------------------------*/
    final private static Logger logger = Logger.getLogger("org.deuce");    
    final private static String ALERT_MSG_UNWRAP = "!!! ALERT: you cannot unwrap a multi-dimensional array from a VBoxObjectArray! Consider to access its elements through an atomic getter indexer or exclude its owner class from the instrumentation.";
    final private static String ALERT_MSG_EXCL_CLASS_WITH_ARRAYS = "ERROR: You cannot pass arrays as arguments of constructors of classes that have been excluded from Deuce instrumentation.!!!";

    final private static String VBOX_BYTE_ARRAY_DESC = Type.getDescriptor(VBoxByteArray.class);
    final private static String VBOX_CHAR_ARRAY_DESC = Type.getDescriptor(VBoxCharArray.class);
    final private static String VBOX_SHORT_ARRAY_DESC = Type.getDescriptor(VBoxShortArray.class);
    final private static String VBOX_INT_ARRAY_DESC = Type.getDescriptor(VBoxIntArray.class);
    final private static String VBOX_LONG_ARRAY_DESC = Type.getDescriptor(VBoxLongArray.class);
    final private static String VBOX_FLOAT_ARRAY_DESC = Type.getDescriptor(VBoxFloatArray.class);
    final private static String VBOX_DOUBLE_ARRAY_DESC = Type.getDescriptor(VBoxDoubleArray.class);
    final private static String VBOX_OBJECT_ARRAY_DESC = Type.getDescriptor(VBoxObjectArray.class);
    
    final private static String VBOX_BYTE_ARRAY_NAME = Type.getInternalName(VBoxByteArray.class);
    final private static String VBOX_CHAR_ARRAY_NAME = Type.getInternalName(VBoxCharArray.class);
    final private static String VBOX_SHORT_ARRAY_NAME = Type.getInternalName(VBoxShortArray.class);
    final private static String VBOX_INT_ARRAY_NAME = Type.getInternalName(VBoxIntArray.class);
    final private static String VBOX_LONG_ARRAY_NAME = Type.getInternalName(VBoxLongArray.class);
    final private static String VBOX_FLOAT_ARRAY_NAME = Type.getInternalName(VBoxFloatArray.class);
    final private static String VBOX_DOUBLE_ARRAY_NAME = Type.getInternalName(VBoxDoubleArray.class);
    final private static String VBOX_OBJECT_ARRAY_NAME = Type.getInternalName(VBoxObjectArray.class);

    
    private static String getVBoxArrayDescriptor(Type t){
	switch( t.getSort()) {
	case Type.BOOLEAN:
	case Type.BYTE:
	    return VBOX_BYTE_ARRAY_DESC;
	case Type.CHAR:
	    return VBOX_CHAR_ARRAY_DESC;
	case Type.SHORT:
	    return VBOX_SHORT_ARRAY_DESC;
	case Type.INT:
	    return VBOX_INT_ARRAY_DESC;
	case Type.LONG:
	    return VBOX_LONG_ARRAY_DESC;
	case Type.FLOAT:
	    return VBOX_FLOAT_ARRAY_DESC;
	case Type.DOUBLE:
	    return VBOX_DOUBLE_ARRAY_DESC;
	default:
	    return VBOX_OBJECT_ARRAY_DESC;
	}
    }
    
    private static String getVBoxArrayInternalName(Type t){
	switch(t.getSort()) {
	case Type.BOOLEAN:
	case Type.BYTE:
	    return VBOX_BYTE_ARRAY_NAME;
	case Type.CHAR:
	    return VBOX_CHAR_ARRAY_NAME;
	case Type.SHORT:
	    return VBOX_SHORT_ARRAY_NAME;
	case Type.INT:
	    return VBOX_INT_ARRAY_NAME;
	case Type.LONG:
	    return VBOX_LONG_ARRAY_NAME;
	case Type.FLOAT:
	    return VBOX_FLOAT_ARRAY_NAME;
	case Type.DOUBLE:
	    return VBOX_DOUBLE_ARRAY_NAME;
	default:
	    return VBOX_OBJECT_ARRAY_NAME;
	}
    }

    private static String getVBoxArrayInternalName(int operand){
	switch( operand) {
	case T_BOOLEAN:
	case T_BYTE:
	    return VBOX_BYTE_ARRAY_NAME;
	case T_CHAR:
	    return VBOX_CHAR_ARRAY_NAME;
	case T_SHORT:
	    return VBOX_SHORT_ARRAY_NAME;
	case T_INT:
	    return VBOX_INT_ARRAY_NAME;
	case T_LONG:
	    return VBOX_LONG_ARRAY_NAME;
	case T_FLOAT:
	    return VBOX_FLOAT_ARRAY_NAME;
	case T_DOUBLE:
	    return VBOX_DOUBLE_ARRAY_NAME;
	default:
	    throw new IllegalStateException();
	}
    }

    /*---------------------------------------------------------*
     *~~~~~~~~~~~~~~~          FIELDS      ~~~~~~~~~~~~~~~~~~~~*
     *---------------------------------------------------------*/    
    String className;
    byte[] classBytecodes; 
    List<MethodInfo> atomicMethods = new LinkedList<MethodInfo>(); // Keeps the atomic methods with arguments of array type.
    Set<MethodTarget> mediatorsForExcludedClasses = new HashSet<MethodTarget>(); // Keeps the invoked methods of excluded classes, with arguments of array type. 

    /*---------------------------------------------------------*
     *~~~~~~~~~~~~~~~        CONSTRUCTOR   ~~~~~~~~~~~~~~~~~~~~*
     *---------------------------------------------------------*/

    /**
     * The visit method from the ClassEnhancer interface implementation 
     * will create a new instance applying this constructor.      
     */    
    public EnhanceVBoxArrays() {
	super(new ClassWriter(0));
    }
    
    /*---------------------------------------------------------*
     *~~~~~~~~~~~~~~  ClassEnhancer Methods   ~~~~~~~~~~~~~~~~~*
     *---------------------------------------------------------*/
    /**
     * If the class corresponding to the classfileBuffer received by argument is not Transactional,
     * then it returns a new List just containing this unmodified class.
     * We considerer all non abstract types as Transactional classes.
     */
    public List<ClassByteCode> visit(boolean offline, String className, byte[] classfileBuffer) {
        List<ClassByteCode> res = new LinkedList<ClassByteCode>();
        if(excludeClasses.contains(className) || !JvstmStorage.checkTransactional(className)){
            res.add(new ClassByteCode(className, classfileBuffer));
        }else{
            this.className = className;
            this.classBytecodes = classfileBuffer;
            ClassReader cr = new ClassReader(classBytecodes);
            try{
                cr.accept(this, 0); // this call will dispatch the invocation to the visit method bellow
                byte[] transformedClass = ((ClassWriter)super.cv).toByteArray();
                res.add(new ClassByteCode(className, transformedClass)); // Adds the transformed class, enhanced with VBoxArrays.
            }catch(StopInstrumentationException e){
                // That's because the transformation process has been interrupted 
                // due to the source class being an interface.
                // In this case return the original untransformed type.
                res.add(new ClassByteCode(className, classfileBuffer));
            }
        }
        return res;
    }

    private static Collection<String> excludeClasses = Arrays.asList(
            "java/lang/Enum", 
            "java/lang/String", 
            "java/lang/ThreadLocal",
            "java/lang/Exception", 
            "java/lang/RuntimeException", 
            "java/lang/Throwable");
       
    /*---------------------------------------------------------*
     *~~~~~~~~~~~~~~  ClassVisitor INTERFACE  ~~~~~~~~~~~~~~~~~*
     *---------------------------------------------------------*/
    /**
     * We intercept all the fields of the type T[] and replace it by  VBox<T>Array   
     */
    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
	if(desc.charAt(0) == '['){
	    if(desc.charAt(1) == '['){
	        // In case of multi-dim array
		desc = VBOX_OBJECT_ARRAY_DESC;
	    }
	    else{
		final Type fieldType = Type.getType(desc);
		Type elemType = fieldType.getElementType();
		desc = getVBoxArrayDescriptor(elemType);
	    }
	    signature = null; // we do not keep parameter type information 
	}
        return super.visitField(access, name, desc, signature, value);
    }
    /**
     * For each method contained in the mediatorsForExcludedClasses structure, 
     * creates a new mediator method, which receives VBoxArray objects as arguments, 
     * unwraps the encapsulated elements array and invokes the target method.
     * For each original atomic method, which receives or return arrays, 
     * creates a third twin that convert arrays in VBox.
     */
    @Override
    public void visitEnd() {
        for(MethodTarget target : mediatorsForExcludedClasses){
            MethodVisitor mv = super.visitMethod(
                    ACC_FINAL | ACC_PRIVATE | ACC_STATIC, 
                    mediatorMethodName(target.owner, target.name), 
                    target.newDesc, 
                    null, // signature, 
                    null // exceptions
                    );
            Type returnType = Type.getReturnType(target.desc);
            Type[] targetArgs = Type.getArgumentTypes(target.desc);
            Type[] currentArgs = Type.getArgumentTypes(target.newDesc);
            if(targetArgs.length != currentArgs.length){
                if(targetArgs.length != (currentArgs.length - 1))
                    throw new IllegalStateException("STRANGE case not covered by this mediator method!");
                Type[] aux = new Type[currentArgs.length];
                System.arraycopy(targetArgs, 0, aux, 1, targetArgs.length);
                aux[0] = currentArgs[0];
                targetArgs = aux;
            }

            TypeCodeResolver returnReolver = TypeCodeResolverFactory.getReolver(returnType);
            TypeCodeResolver[] argumentReolvers = new TypeCodeResolver[targetArgs.length];
            for( int i=0; i< targetArgs.length ; ++i) {
                argumentReolvers[ i] = TypeCodeResolverFactory.getReolver( targetArgs[ i]);
            }

            // load the rest of the arguments
            int local = 0;
            for( int i=0 ; i < argumentReolvers.length ; ++i) { 
                mv.visitVarInsn(argumentReolvers[i].loadCode(), local);
                if(targetArgs[i].getSort() == Type.ARRAY){
                    // In this case we have to unwrap the array from the VBoxArray object
                    String capMemName = null;
                    if(targetArgs[i].getDescriptor().charAt(1) == '['){ //multiarray
                        capMemName = VBOX_OBJECT_ARRAY_NAME;
                    }else{
                        capMemName = getVBoxArrayInternalName(targetArgs[i].getElementType());
                    }

                    // Test if the result is null
                    mv.visitInsn(Opcodes.DUP); // ref, ref->
                    Label l0 = new Label();
                    mv.visitJumpInsn(IFNULL, l0); // ref ->
                    Label l1 = new Label();
                    mv.visitJumpInsn(GOTO, l1);
                    mv.visitLabel(l0);
                    mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                    mv.visitInsn(Opcodes.POP); // ->
                    mv.visitInsn(ACONST_NULL); // null ->
                    Label l2 = new Label();
                    mv.visitJumpInsn(GOTO, l2);
                    mv.visitLabel(l1);
                    mv.visitFrame(Opcodes.F_APPEND,1, new Object[] {capMemName}, 0, null);
                    if(targetArgs[i].getDescriptor().charAt(1) == '['){ //multi-array
                        mv.visitTypeInsn(NEW, "java/lang/UnsupportedOperationException");
                        mv.visitInsn(DUP);
                        mv.visitLdcInsn(ALERT_MSG_UNWRAP);
                        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/UnsupportedOperationException", "<init>", "(Ljava/lang/String;)V");
                        mv.visitInsn(ATHROW);
                        /*
                        mv.visitMethodInsn(INVOKEVIRTUAL, VBOX_OBJECT_ARRAY_NAME, "unwrapp", "()Ljava/lang/Object;");
                        mv.visitTypeInsn( CHECKCAST, targetArgs[i].getInternalName());
                        */
                    }else if(capMemName == VBOX_OBJECT_ARRAY_NAME){
                        mv.visitFieldInsn(GETFIELD, capMemName, "elements", "[Ljava/lang/Object;"); // array ->
                        mv.visitTypeInsn( CHECKCAST, targetArgs[i].getInternalName());
                    }
                    else{
                        mv.visitFieldInsn(GETFIELD, capMemName, "elements", targetArgs[i].getDescriptor()); // array ->
                    }
                    mv.visitLabel(l2);
                }
                local += argumentReolvers[i].localSize(); // move to the next argument
            }

            // invoke the corresponding method with Arrays as argument instead of VBoxArray 
            mv.visitMethodInsn(target.opcode, target.owner, target.name, target.desc);

            // returns
            if( returnReolver == null) {
                mv.visitInsn( RETURN); // return;
            }
            else {
                if(returnType.getSort() == Type.ARRAY){
                    // In this case we have to wrap the array into a VBoxArray object
                    String capMemName = getVBoxArrayInternalName(returnType.getElementType());
                    // stack = array ->
                    mv.visitTypeInsn(NEW, capMemName);// array, ref ->
                    mv.visitInsn(Opcodes.DUP_X1);// ref, array, ref ->
                    mv.visitInsn(Opcodes.SWAP); // ref, ref, array ->
                    String desc = null;
                    if(capMemName == VBOX_OBJECT_ARRAY_NAME){
                        desc = "[Ljava/lang/Object;";
                    }else{
                        desc = returnType.getDescriptor(); 
                    }
                    mv.visitMethodInsn(INVOKESPECIAL, capMemName, "<init>", "(" + desc +")V"); // ref ->
                    mv.visitInsn(returnReolver.returnCode()); // RETURN
                }else{
                    mv.visitInsn(returnReolver.returnCode()); // RETURN
                }
            }
            int varsSize = variablesSize( argumentReolvers, true);
            mv.visitMaxs(6 + varsSize , varsSize);
            mv.visitEnd();
        }
        for (MethodInfo mInfo : atomicMethods) {
	    MethodVisitor mv = super.visitMethod(mInfo.access, mInfo.methodName, mInfo.desc, mInfo.signature, mInfo.exceptions);

	    Type returnType = Type.getReturnType(mInfo.desc);
	    Type[] argumentTypes = Type.getArgumentTypes(mInfo.desc);

	    TypeCodeResolver returnReolver = TypeCodeResolverFactory.getReolver(returnType);
	    TypeCodeResolver[] argumentReolvers = new TypeCodeResolver[ argumentTypes.length];
	    for( int i=0; i< argumentTypes.length ; ++i) {
		argumentReolvers[ i] = TypeCodeResolverFactory.getReolver( argumentTypes[ i]);
	    }

	    // load the rest of the arguments
	    boolean isStatic = (mInfo.access & ACC_STATIC) != 0;
	    if(!isStatic)
		mv.visitVarInsn(ALOAD, 0);
	    
	    // load the rest of the arguments
	    int local = isStatic ? 0 : 1;
	    for( int i=0 ; i < argumentReolvers.length ; ++i) { 
		mv.visitVarInsn(argumentReolvers[i].loadCode(), local);
		if(argumentTypes[i].getSort() == Type.ARRAY){
		    // In this case we have to wrap the array into a VBox object
		    String capMemName = null;
		    if(argumentTypes[i].getDescriptor().charAt(1) == '['){ //multiarray
			capMemName = VBOX_OBJECT_ARRAY_NAME;
		    }else{
			capMemName = getVBoxArrayInternalName(argumentTypes[i].getElementType());
		    }
		    
		    // stack = array ->
		    mv.visitTypeInsn(NEW, capMemName);// array, ref ->
		    mv.visitInsn(Opcodes.DUP_X1);// ref, array, ref ->
		    mv.visitInsn(Opcodes.SWAP); // ref, ref, array ->
		    String desc = null;
		    if(capMemName == VBOX_OBJECT_ARRAY_NAME){
			desc = "[Ljava/lang/Object;";
		    }else{
			desc = argumentTypes[i].getDescriptor(); 
		    }
		    mv.visitMethodInsn(INVOKESPECIAL, capMemName, "<init>", "(" + desc +")V"); // ref ->
    
		}
		local += argumentReolvers[i].localSize(); // move to the next argument
	    }

	    // invoke the corresponding method with VBoxArray as argument instead of array. 
	    if( isStatic)
		mv.visitMethodInsn(INVOKESTATIC, className, mInfo.methodName, replaceArgumentsOfArrayByVBox(mInfo.desc)); // ... = foo( ...
	    else
		mv.visitMethodInsn(INVOKEVIRTUAL, className, mInfo.methodName, replaceArgumentsOfArrayByVBox(mInfo.desc)); // ... = foo( ...

	    // returns
	    if( returnReolver == null) {
		mv.visitInsn( RETURN); // return;
	    }
	    else {
		if(returnType.getSort() == Type.ARRAY){
		    // In this case we have to unwrap the array from the VBox object
		    String capMemName = getVBoxArrayInternalName(returnType.getElementType());

		    // Test if the result is null
		    mv.visitVarInsn(ASTORE, local); // last argument => next a new local variable
		    mv.visitVarInsn(ALOAD, local);
		    Label l0 = new Label();
		    mv.visitJumpInsn(IFNONNULL, l0); // Test if the result is NULL
		    mv.visitInsn(ACONST_NULL);
		    mv.visitInsn(ARETURN); // RETURN NULL
		    mv.visitLabel(l0);
		    mv.visitFrame(Opcodes.F_APPEND,1, new Object[] {capMemName}, 0, null);
		    mv.visitVarInsn(ALOAD, local);
		    if(returnType.getDescriptor().charAt(1) == '['){ //multi-array
		        mv.visitTypeInsn(NEW, "java/lang/UnsupportedOperationException");
		        mv.visitInsn(DUP);
		        mv.visitLdcInsn(ALERT_MSG_UNWRAP);
		        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/UnsupportedOperationException", "<init>", "(Ljava/lang/String;)V");
		        mv.visitInsn(ATHROW);
		        /*
			mv.visitMethodInsn(INVOKEVIRTUAL, VBOX_OBJECT_ARRAY_NAME, "unwrapp", "()Ljava/lang/Object;");
			mv.visitTypeInsn( CHECKCAST, returnType.getInternalName());
			*/
		    }else if(capMemName == VBOX_OBJECT_ARRAY_NAME){
			mv.visitFieldInsn(GETFIELD, capMemName, "elements", "[Ljava/lang/Object;"); // array ->
			mv.visitTypeInsn( CHECKCAST, returnType.getInternalName());
		    }
		    else{
			mv.visitFieldInsn(GETFIELD, capMemName, "elements", returnType.getDescriptor()); // array ->
		    }
		    mv.visitInsn(returnReolver.returnCode()); // RETURN
		}else{
		    mv.visitInsn(returnReolver.returnCode()); // RETURN
		}
	    }
	    int varsSize = variablesSize( argumentReolvers, isStatic) + 1; // add 1 for the auxiliar local variable
	    mv.visitMaxs(6 + varsSize , varsSize);
	    mv.visitEnd();
	}
    }
    private static int variablesSize(TypeCodeResolver[] types, boolean isStatic) {
	int i = isStatic ? 0 : 1;
	for( TypeCodeResolver type : types) {
		i += type.localSize();
	}
	return i;
    }
    /**
     * For transactional methods replaces arguments of array type by the corresponding
     * VBoxArray wrapper. 
     */
    @Override
    public MethodVisitor visitMethod(int access,final String methodName,String desc,String signature,String[] exceptions){	
	String oldDesc = desc; // Keep the original descriptor
	
	/*
	 * It returns a negative value if this method does not contain 
	 * a Context argument - original regular method.
	 * Otherwise, this method includes a Context argument and this means
	 * that it is a transactional method.
	 */
	int argumentsSize = ctorArgumentsSize(access, desc);
	if(argumentsSize >= 0){
	    desc = replaceArgumentsOfArrayByVBox(desc);
	}
	
	MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);

	if(argumentsSize >= 0){
	    // In this case this is a transactional method
	    MethodInfo mInfo = null;
	    if(!desc.equals(oldDesc)){
	        /*
	         * In this case the transactional method has arguments of array types and 
	         * if it is also Atomic, then we have to create a 3rd method. 
	         * Otherwise, we do not have to change anything in the method definition.
	         */
		mInfo = new MethodInfo(access, methodName, oldDesc, signature, exceptions);
	    }
	    mv = replaceArraysAccessesByVBox(mv, argumentsSize, mInfo);
	}else{
	    // In this case this is the original regular method
	    mv = unwrapArrayFromVBox(mv, argumentsSize, methodName);
	}
	
        return mv;
    }
    
    /**
     * All arrays fields are replaced by VBox<T>Array and in non-transactional 
     * methods we must unwrap those arrays from the wrapper object.
     */
    private MethodVisitor unwrapArrayFromVBox(MethodVisitor mv, final int argumentsSize, final String methodName){
	return new MethodAdapter(mv) {
	    private boolean replaceAccessField = false;
	    /**
	     * Replaces getfield, putfield, putstatic and getstatic of fields of array type,
	     * by the correspondent descriptor on VBox.
	     */
	    @Override
	    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		if(desc.charAt(0) == '['){
		    replaceAccessField = true;
		    String capMemName, capMemDesc = null;
		    if(desc.charAt(1) == '['){
			capMemName = VBOX_OBJECT_ARRAY_NAME;
			capMemDesc = VBOX_OBJECT_ARRAY_DESC;
		    }else{
			capMemName = getVBoxArrayInternalName(Type.getType(desc).getElementType());
			capMemDesc = getVBoxArrayDescriptor(Type.getType(desc).getElementType());
		    }
		    if(opcode == PUTFIELD || opcode == PUTSTATIC){
			// stack = array ->
			super.visitTypeInsn(NEW, capMemName);// array, ref ->
			super.visitInsn(Opcodes.DUP_X1);// ref, array, ref ->
			super.visitInsn(Opcodes.SWAP); // ref, ref, array ->
			if(capMemName == VBOX_OBJECT_ARRAY_NAME){
			    desc = "[Ljava/lang/Object;";
			}else if(desc.charAt(1) == 'Z'){
                            desc = desc.replace('Z', 'B');
                        }
			super.visitMethodInsn(INVOKESPECIAL, capMemName, "<init>", "(" + desc +")V"); // ref ->
			super.visitFieldInsn(opcode, owner, name, capMemDesc); // ->
			return;
		    }else if(opcode == GETFIELD || opcode == GETSTATIC){
			super.visitFieldInsn(opcode, owner, name, capMemDesc); // ref ->
			/*
			 * Test if the result is null
			 */
			super.visitInsn(Opcodes.DUP); // ref, ref->
                        Label l0 = new Label();
                        super.visitJumpInsn(IFNULL, l0); // ref ->
                        Label l1 = new Label();
                        super.visitJumpInsn(GOTO, l1);
                        super.visitLabel(l0);
                        super.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                        super.visitInsn(Opcodes.POP); // ->
                        super.visitInsn(ACONST_NULL); // nul ->
                        Label l2 = new Label();
                        super.visitJumpInsn(GOTO, l2);
                        super.visitLabel(l1);
                        super.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {capMemName}); // array ->
			if(desc.charAt(1) == '['){//multi-array
			    mv.visitTypeInsn(NEW, "java/lang/UnsupportedOperationException");
			    mv.visitInsn(DUP);
			    mv.visitLdcInsn(ALERT_MSG_UNWRAP);
			    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/UnsupportedOperationException", "<init>", "(Ljava/lang/String;)V");
			    mv.visitInsn(ATHROW);
			    /*
			    super.visitMethodInsn(INVOKEVIRTUAL, VBOX_OBJECT_ARRAY_NAME, "unwrapp", "()Ljava/lang/Object;");
			    super.visitTypeInsn( CHECKCAST, Type.getType(desc).getInternalName());
			    */
			}
			else if(capMemName == VBOX_OBJECT_ARRAY_NAME){
			    super.visitFieldInsn(GETFIELD, capMemName, "elements", "[Ljava/lang/Object;"); // array ->
			    super.visitTypeInsn( CHECKCAST, Type.getType(desc).getInternalName());
			}
			else{  
			    if(desc.charAt(1) == 'Z'){
			        desc = desc.replace('Z', 'B');
			    }
			    super.visitFieldInsn(GETFIELD, capMemName, "elements", desc); // array ->
			}
			super.visitLabel(l2);
			return;
		    }
		}
		super.visitFieldInsn(opcode, owner, name, desc);
	    }
	    @Override
	    public void visitMaxs(int maxStack, int maxLocals) {
		if(replaceAccessField)
		    maxStack += 6;
	        super.visitMaxs(maxStack, maxLocals);
	    }
	};
    }
    
    /**
     * For transactional methods we replace all array accesses T[] by VBox<T>Array. 
     */
    private MethodVisitor replaceArraysAccessesByVBox(MethodVisitor mv, final int argumentsSize, final MethodInfo mInfo){
	return new MethodAdapter(mv) {
	    
	    boolean isIrrevocable = false;
            
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if(Type.getType(desc).getClassName().equals(Irrevocable.class.getName())){
                    isIrrevocable = true; 
                }
	        if(mInfo != null && Type.getType(desc).getClassName().equals(Atomic.class.getName())){
	            atomicMethods.add(mInfo); // For later create the 3rd twin.
	        }
	        return super.visitAnnotation(desc, visible);
	    }

	    /**
	     * Replaces local variables declaration.
	     */
	    @Override
	    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
		if(desc.charAt(0) == '['){
		    if(desc.charAt(1) == '['){
			desc = VBOX_OBJECT_ARRAY_DESC;
		    }
		    else{
			final Type fieldType = Type.getType(desc);
			Type elemType = fieldType.getElementType();
			desc = getVBoxArrayDescriptor(elemType);
		    }
		    signature = null; // we do not keep parameter type information 
		}
	        super.visitLocalVariable(name, desc, signature, start, end, index);
	    }
	    
	    /**
	     * Replaces getfield, putfield, putstatic and getstatic of fields of array type,
	     * by the correspondent descriptor on VBox.
	     */
	    @Override
	    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		if(desc.charAt(0) == '['){
		    if(desc.charAt(1) == '['){
			desc = VBOX_OBJECT_ARRAY_DESC;
		    }
		    else{
			final Type fieldType = Type.getType(desc);
			Type elemType = fieldType.getElementType();
			desc = getVBoxArrayDescriptor(elemType);
		    }
		}
	        super.visitFieldInsn(opcode, owner, name, desc);
	    }
	    
	    /**
	     * On array length unwrap array from VBox.
	     */
	    @Override
	    public void visitInsn(int opcode) {
		if(opcode == ARRAYLENGTH)
		    super.visitMethodInsn(INVOKEVIRTUAL, AbstractVBoxArray.class.getName().replace('.', '/'), "arrayLength", "()I");
		else
		    super.visitInsn(opcode);
	    }
	    
	    /**
	     * Replaces object arrays instantiation.
	     */
	    @Override
	    public void visitTypeInsn(int opcode, String type) {
		if(opcode == ANEWARRAY){
	            super.visitTypeInsn(NEW, VBOX_OBJECT_ARRAY_NAME);// length, ref ->
	            super.visitInsn(Opcodes.DUP_X1);// ref, length, ref ->
	            super.visitInsn(Opcodes.SWAP);// ref, ref, length ->
	            super.visitLdcInsn(type);// ref, ref, length, type ->
	            super.visitVarInsn(ALOAD, argumentsSize - 1); // ref, ref, length, type, ctx ->
                    super.visitTypeInsn( CHECKCAST, CLASS_JVSTM_CTX);
                    super.visitFieldInsn(GETFIELD, CLASS_JVSTM_CTX, FIELD_NAME_CURRENT_TRX, CLASS_JVSTM_TRX_DESC); // ref, ref, length, type, trx ->
	            super.visitMethodInsn(INVOKESPECIAL, VBOX_OBJECT_ARRAY_NAME, "<init>", "(ILjava/lang/String;" + CLASS_JVSTM_TRX_DESC + ")V");
		}
		/**
		 * Intercept operations for CHECKCAST or INSTANCEOF.
		 */
		else if(type.charAt(0) == '['){
		    if(type.charAt(1) == '['){
			type = VBOX_OBJECT_ARRAY_NAME;
		    }
		    else{
			final Type fieldType = Type.getType(type);
			Type elemType = fieldType.getElementType();
			type = getVBoxArrayInternalName(elemType);
		    }
		    super.visitTypeInsn(opcode, type);
		}
		else{
		    super.visitTypeInsn(opcode, type);	        
		}
	    }
	    
	    /**
	     * Replaces multi-arrays instantiation.
	     */
	    @Override
	    public void visitMultiANewArrayInsn(String desc, int dims) {
	        super.visitMultiANewArrayInsn(desc, dims); // multiarr
	        super.visitTypeInsn(NEW, VBOX_OBJECT_ARRAY_NAME);// multiarr, ref ->
	        super.visitInsn(Opcodes.DUP_X1);// ref, multiarr, ref ->
	        super.visitInsn(Opcodes.SWAP);// ref, ref, multiarr->
	        super.visitVarInsn(ALOAD, argumentsSize - 1); // ref, ref, multiarr, ctx ->
                super.visitTypeInsn( CHECKCAST, CLASS_JVSTM_CTX);
                super.visitFieldInsn(GETFIELD, CLASS_JVSTM_CTX, FIELD_NAME_CURRENT_TRX, CLASS_JVSTM_TRX_DESC);// ref, ref, multiarr, trx->
	        super.visitMethodInsn(INVOKESPECIAL, VBOX_OBJECT_ARRAY_NAME, "<init>", "([Ljava/lang/Object;" + CLASS_JVSTM_TRX_DESC + ")V");
	    }
	    
	    /**
	     * Replaces arrays of primitive types.
	     */
	    @Override
	    public void visitIntInsn(int opcode, int operand) {
	        if(opcode == NEWARRAY){
	            // stack = length ->
	            String capMemName= getVBoxArrayInternalName(operand);
	            super.visitTypeInsn(NEW, capMemName);// length, ref ->
	            super.visitInsn(Opcodes.DUP_X1);// ref, length, ref ->
	            super.visitInsn(Opcodes.SWAP);// ref, ref, length ->
	            super.visitVarInsn(ALOAD, argumentsSize - 1); // ref, ref, length, ctx ->
	            super.visitTypeInsn( CHECKCAST, CLASS_JVSTM_CTX);
	            super.visitFieldInsn(GETFIELD, CLASS_JVSTM_CTX, FIELD_NAME_CURRENT_TRX, CLASS_JVSTM_TRX_DESC);// ref, ref, length, trx->
	            super.visitMethodInsn(INVOKESPECIAL, capMemName, "<init>", "(I" + CLASS_JVSTM_TRX_DESC + ")V");
	        }else{
	            super.visitIntInsn(opcode, operand);
	        }
	    }
	    
	    /**
	     * Replaces the invocation of array barriers by the correspondent 
             * methods with VBoxArray as argument.
             * Replaces arguments of array type in method invocation, by the
             * corresponding VBoxArray arguments. 
	     */
	    @Override
	    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
	        if(owner.equals(ContextDelegator.CONTEXT_DELEGATOR_INTERNAL)){
	            if(name.equals(ContextDelegator.READ_ARR_METHOD_NAME) || name.equals(ContextDelegator.WRITE_ARR_METHOD_NAME)){
	        	// Get the first argument descriptor corresponding to an array type
	        	String  firstArgDesc = DescUtil.argsDescIterator(desc).iterator().next();
	        	Type elemType = Type.getType(firstArgDesc).getElementType();
	        	String newArgDesc = getVBoxArrayDescriptor(elemType);
	        	desc = desc.replace(firstArgDesc, newArgDesc);
	        	super.visitMethodInsn(opcode, owner, name, desc);
	        	return;
	            }
	        }
	        if(owner.charAt(0) == '['){
                    if(owner.charAt(1) == '['){
                        // In case of multi-dim array
                        owner = VBOX_OBJECT_ARRAY_NAME;
                    }
                    else{
                        final Type fieldType = Type.getType(desc);
                        Type elemType = fieldType.getElementType();
                        owner = getVBoxArrayInternalName(elemType);
                    } 
                }
	        
	        /*
	         * System.arraycopy has Object arguments instead of [] and does not 
	         * satisfies the next verification.
	         * So we must differentiate the case of the System.arraycopy .
	         */
                if((owner.equals("java/lang/System") && name.equals("arraycopy"))){
                    if(!isIrrevocable && !ExcludeIncludeStore.exclude("java/lang/System")){
                        /*
                         * In this case the system class was not excluded and its methods
                         * are invoke with the additional parameter Context.
                         * So we must remove the Context object from the top of the Stack,
                         * because the arraycopy of the AbstractVBoxArray does not receive 
                         * the Context argument.
                         */
                        mv.visitInsn(Opcodes.POP);
                    }
                    super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(AbstractVBoxArray.class), "arraycopy", "(ILjava/lang/Object;II)V");
                }else{
                    String newDesc = replaceArgumentsOfArrayByVBox(desc);
                    if(ExcludeIncludeStore.exclude(owner) && !desc.equals(newDesc)){
                        /*
                         * In this case the owner of the invoked method is not transactional and
                         * simultaneously it receives arrays arguments that have been wrapped in 
                         * CapturedStateArray objects.
                         * So we will invoke a bridge/mediator method, with the name owner$name, 
                         * which receives VBoxArray objects as arguments, unwraps the encapsulated 
                         * elements arrays and finally invokes the target method.     
                         */ 
                        if(name.equals("<init>")){
                            /*
                             * In this case we cannot separate the INVOKESPECIAL to the <init>
                             * from the previously NEW. Otherwise we will get in the mediator 
                             * the following error:VerifyError: Expecting to find unitialized 
                             * object on stack.
                             * So abort it and advise to remove array parameters from the invoke 
                             * constructor. 
                             */
                            logger.severe(
                                    "Trying to instantiate: " + owner + desc + " from " + className + " - " +
                                    ALERT_MSG_EXCL_CLASS_WITH_ARRAYS);
                            System.exit(-1);
                        }
                        int originalOpcode = opcode; 
                        if(opcode != INVOKESTATIC){
                            opcode = INVOKESTATIC;
                            newDesc = appendArgument(newDesc, "L" + owner + ";");
                        }
                        mediatorsForExcludedClasses.add(new MethodTarget(originalOpcode, owner, name, desc, newDesc));
                        name = mediatorMethodName(owner, name);
                        owner = className;
                    }
                    super.visitMethodInsn(opcode, owner, name, newDesc);
                }
	    }
	    @Override
	    public void visitMaxs(int maxStack, int maxLocals) {
	        super.visitMaxs(maxStack + 6, maxLocals);
	    }
	};
    }
    
    
    private static int ctorArgumentsSize(int access, String methodDesc){
	int size = 0;
	String lastArgDesc = null;
	for (String  d: DescUtil.argsDescIterator(methodDesc)) {
	    lastArgDesc = d;
	    size += d.equals("J") || d.equals("D")? 2 : 1;
	}
	if(size == 0 || !lastArgDesc.equals(Context.CONTEXT_DESC))
	    return -1;
	else{
	    if((access & ACC_STATIC) == 0)
		size++;// include the 'this' argument
	    return size; 
	}
    }
    
    /**
     * For transactional methods replace all arguments in descriptor 
     * from array type to VBoxArray
     */
    private static String replaceArgumentsOfArrayByVBox(String desc){
	String newNewthodDesc = "("; 
	for (String  d: DescUtil.argsDescIterator(desc)) {
	    if(d.charAt(0) == '['){
		if(d.charAt(1) == '['){// multiarray
		    newNewthodDesc += VBOX_OBJECT_ARRAY_DESC;
		} else {
		    newNewthodDesc += getVBoxArrayDescriptor(Type.getType(d).getElementType());
		}
	    }else{
		newNewthodDesc += d;
	    }
	}
	newNewthodDesc += ")";

	// replace the return type from array to VBoxArray
	int idxRet = desc.indexOf(')') + 1;
	String retDesc = desc.substring( idxRet);
	if(retDesc.charAt(0) == '['){
	    if(retDesc.charAt(1) == '['){// multiarray
		retDesc = VBOX_OBJECT_ARRAY_DESC;
	    } else {
		retDesc = getVBoxArrayDescriptor(Type.getType(retDesc).getElementType());
	    }
	}
	return newNewthodDesc + retDesc;
    }
    private static String appendArgument(String methodDesc, String argDesc){
        return "(" + argDesc + methodDesc.substring(1);
    }
}
