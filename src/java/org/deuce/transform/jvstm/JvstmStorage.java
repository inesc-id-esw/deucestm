package org.deuce.transform.jvstm;

import org.deuce.transform.Exclude;
import org.deuce.transform.util.IgnoreTree;

/**
 * According to the EnhanceTransactional transformation for the JVSTM, all the transactional
 * classes should provide a versioned history.
 * This requirement is enhanced through the modification of the top of the classes hierarchy 
 * from Object to VBox. Yet, not all classes could inherit from VBox and in those cases we could 
 * bound the set of transactional classes specifying the transactional candidates through
 * the system property org.deuce.transform.jvstm.trxClasses.
 * We call it candidates because the abstract type are excluded. 
 * By default all loaded classes are transactional candidates.
 *
 * @author mcarvalho
 */
@Exclude
public class JvstmStorage {
    final static private IgnoreTree transactionalCandidates, notTrx;  
    static{
        /*
         * Transactional candidates.
         */
	String property = System.getProperty("org.deuce.transform.jvstm.trxClasses");
	if(property == null || property == ""){
	    // property = "java.lang.AbstractStringBuilder,java.lang.StringBuffer,java.lang.StringBuilder,java.util.*";
	    property = "*";
	}
	transactionalCandidates = new IgnoreTree( property);
	/*
	 * Not transactional classes.
	 */
	property = System.getProperty("org.deuce.transform.jvstm.notTrx");
	notTrx = new IgnoreTree( property);
    }
    public static boolean checkTransactional(String className){
	// We replace $ by / to deal with inner classes as subpackages.
        className = className.replace('$', '/');
        if(notTrx.contains(className)) 
            return false;
	return transactionalCandidates.contains(className);
    }
}
