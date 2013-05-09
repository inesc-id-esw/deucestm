package org.deuce.utest.jvstm.capmem;

import org.deuce.Atomic;
import org.deuce.transform.Exclude;
import org.junit.Test;

import junit.framework.Assert;

/**
 * This test must be ran with the following System properties:
 * -javaagent:bin/deuceAgent.jar
 * -Dorg.deuce.delegator=org.deuce.transaction.jvstm.ContextDelegator
 * -Dorg.deuce.exclude=org.junit.*,org.deuce.utest.capmem.AvoidBarriers,java.lang.Integer,java.lang.String,java.lang.reflect.*,sun.reflect.*,jvstm.*
 *   
 * @author mcarvalho
 */
@Exclude
public class NonTransactionalArraysForJvstmTest{
	@Test
	public void testOnarrays(){
		String s = NonTransactionalArraysTestForJvstmTarget.makeArray(new CharArrayStorage());
		Assert.assertEquals("matias", s);
	}

}


class CharArrayStorage{
	char [] elems;

	public CharArrayStorage() {
		this.elems = new char[]{'m', 'a', 't', 'i', 'a', 's'};
	}    
}


class NonTransactionalArraysTestForJvstmTarget{
	@Atomic
	public static String makeArray(){
		CharArrayStorage arr = new CharArrayStorage();
		return String.copyValueOf(arr.elems);
	}

	@Atomic
	public static String makeArray(CharArrayStorage arr){
		return String.copyValueOf(arr.elems);
	}   

}