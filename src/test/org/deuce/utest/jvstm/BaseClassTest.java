package org.deuce.utest.jvstm;

import java.lang.reflect.ParameterizedType;
import java.util.AbstractCollection;

import junit.framework.Assert;
import jvstm.VBoxAom;

import org.deuce.transform.Exclude;
import org.deuce.utest.point.jvstm.IntPoint;
import org.junit.Test;

class DummyTransactional{
}

/**
 * This test must be run with following configurations of JVM parameters:
 * -Xbootclasspath:lib/rt7-jvstm.jar;lib/jvstm-lf-aom.jar;bin/deuceAgent.jar 
 * -javaagent:bin/deuceAgent.jar
 * -Dorg.deuce.transform.jvstm.trxClasses=org.deuce.utest.jvstm.DummyTransactional,org.deuce.utest.point.impl.AomIntPoint
 * -Dorg.deuce.transform.pos=org.deuce.transform.jvstm.EnhanceTransactional 
 * -Dorg.deuce.exclude="sun.*,java.lang.reflect.*,java.lang.*,jvstm.*,jvstm.gc.*,jvstm.util.*"
 * 
 * Tests if the Deuce engine together with the pos transformation jvstm.EnhanceTransactional
 * change the super class from Object to VBox.
 * 
 * @author Fernando Miguel Carvalho
 */
@Exclude
public class BaseClassTest {

	@Test
	public void testAomIntPoint(){
		//
		// Check if the base class is VBoxAom
		//
		Assert.assertEquals(VBoxAom.class, IntPoint.class.getSuperclass());	
		//
		// Check if the VBox super class is parameterized with DummyTransactional  
		//
		ParameterizedType superType = (ParameterizedType) IntPoint.class.getGenericSuperclass();
		Class actualType = (Class) superType.getActualTypeArguments()[0];
		Assert.assertEquals(IntPoint.class, actualType);

	}

	@Test
	public void testDummyClass(){
		//
		// Check if the base class is VBoxAom
		//
		Assert.assertEquals(VBoxAom.class, DummyTransactional.class.getSuperclass());	
		//
		// Check if the VBox super class is parameterized with DummyTransactional  
		//
		ParameterizedType superType = (ParameterizedType) DummyTransactional.class.getGenericSuperclass();
		Class actualType = (Class) superType.getActualTypeArguments()[0];
		Assert.assertEquals(DummyTransactional.class, actualType);

	}

	@Test
	public void testJreClasses(){
		//
		// Check if the base class is VBoxAom
		//
		Assert.assertEquals(VBoxAom.class, AbstractCollection.class.getSuperclass());	
		//
		// Check if the VBoxAom super class is parameterized with AbstractCollection  
		//
		ParameterizedType superType = (ParameterizedType) AbstractCollection.class.getGenericSuperclass();
		Class actualType = (Class) superType.getActualTypeArguments()[0];
		Assert.assertEquals(AbstractCollection.class, actualType);

	} 
}
