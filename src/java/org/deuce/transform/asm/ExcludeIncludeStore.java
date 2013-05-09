package org.deuce.transform.asm;

import java.util.HashSet;

import org.deuce.transaction.AbortTransactionException;
import org.deuce.transaction.TransactionException;
import org.deuce.transform.util.IgnoreTree;

/**
 * Holds the include/exclude information for the classes to instrument.
 *  
 * @author guy
 * @since 1.1
 */
public class ExcludeIncludeStore {

	final private IgnoreTree excludeTree;
	final private IgnoreTree includeTree;
	final private HashSet<String> excludeClass = new HashSet<String>();
	final private HashSet<String> immutableClass = new HashSet<String>();
	
	final private static ExcludeIncludeStore excludeIncludeStore = new ExcludeIncludeStore();
	static{
		excludeIncludeStore.excludeClass.add("java/lang/Object");
		excludeIncludeStore.excludeClass.add("java/lang/Thread");
		excludeIncludeStore.excludeClass.add("java/lang/Throwable");
		excludeIncludeStore.excludeClass.add("java/lang/Void");
		excludeIncludeStore.excludeClass.add("javax/management/remote/rmi/_RMIConnection_Stub");
		excludeIncludeStore.excludeClass.add("org/omg/stub/javax/management/remote/rmi/_RMIConnection_Stub");
		//Always ignore TransactionException so user can explicitly throw this exception
		excludeIncludeStore.excludeClass.add(TransactionException.TRANSACTION_EXCEPTION_INTERNAL);
		excludeIncludeStore.excludeClass.add(AbortTransactionException.ABORT_TRANSACTION_EXCEPTION_INTERNAL);
		
		excludeIncludeStore.immutableClass.add("java/lang/String");
		excludeIncludeStore.immutableClass.add("java/lang/BigDecimal");
		excludeIncludeStore.immutableClass.add("java/lang/BigInteger");
		excludeIncludeStore.immutableClass.add("java/lang/Byte");
		excludeIncludeStore.immutableClass.add("java/lang/Double");
		excludeIncludeStore.immutableClass.add("java/lang/Float");
		excludeIncludeStore.immutableClass.add("java/lang/Integer");
		excludeIncludeStore.immutableClass.add("java/lang/Long");
		excludeIncludeStore.immutableClass.add("java/lang/Short");
	}

	
	private ExcludeIncludeStore(){

		String property = System.getProperty("org.deuce.exclude");
		if( property == null)
			property = "java.*,sun.*,org.eclipse.*,org.junit.*,junit.*";
		excludeTree = new IgnoreTree( property);
		
		/*
		 * We cannot add the Exclude annotation to inner classes generated 
		 * by the compiler (e.g. $1 state machine for switch statement).
		 * So we must exclude the Deuce transaction infrastructure via
		 * the excludeTree.
		 */
		excludeTree.add("org.deuce.transaction.*");   
		
		property = System.getProperty("org.deuce.include");
		if( property == null)
			property = "";
		includeTree = new IgnoreTree( property);
	}
	
	/**
	 * There are some immutable objects such as, String, Integer, etc, 
	 * that could be excluded from Deuce instrumentation. Yet, when 
	 * these objects are accessed through their interfaces–e.g. Comparable, 
	 * which is still transactional–it means that Deuce will invoke the 
	 * transactional version of its methods–with an additional parameter 
	 * Context. However, if the subclasses of those interfaces are not 
	 * transactional, then we will get an AbstractMethodError regarding 
	 * the absence of the methods with the Context parameter.
	 * So we added a new semantic to the ExcludeIncludeStore corresponding 
	 * to Immutable classes – i.e. instrumented classes containing a 
	 * transactional version of its methods but without performing STM 
	 * barriers. 
	 * This way we avoid the previous error and still enable the immutable 
	 * classes semantics.
	 */
	public static boolean immutable(String className){
	    return excludeIncludeStore.immutableClass.contains(className);
	}
	
	public static boolean exclude(String className){
		if(excludeIncludeStore.excludeClass.contains(className))
			return true;

		// We replace $ by / to deal with inner classes as subpackages.
		className = className.replace('$', '/');

		return excludeIncludeStore.excludeTree.contains(className) && !excludeIncludeStore.includeTree.contains(className);
	} 
	
}
