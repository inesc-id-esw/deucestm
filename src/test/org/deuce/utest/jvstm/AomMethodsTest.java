package org.deuce.utest.jvstm;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Hashtable;

import junit.framework.Assert;
import jvstm.VBox;

import org.deuce.transform.Exclude;
import org.deuce.utest.point.jvstm.IntPoint;
import org.junit.Test;

class Xpto{
	final short a;
	final int b;
	final long c;
	final byte d;
	final String e;
	final Double f;
	public Xpto(short a, int b, long c, byte d, String e, Double f) {
		super();
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
		this.f = f;
	}
}

/**
 * This test must be run with following configurations of JVM parameters:
 * -Xbootclasspath:lib/rt7-jvstm.jar;lib/jvstm-lf-aom.jar;bin/deuceAgent.jar 
 * -javaagent:bin/deuceAgent.jar
 * -Dorg.deuce.transform.jvstm.trxClasses="org.deuce.utest.jvstm.DummyTransactional,org.deuce.utest.point.impl.AomIntPoint"
 * -Dorg.deuce.transform.pos=org.deuce.transform.jvstm.EnhanceTransactional 
 * -Dorg.deuce.exclude="sun.*,java.lang.reflect.*,java.lang.Thread,java.lang.System,java.lang.Class,java.lang.Enum,jvstm.*,jvstm.gc.*,jvstm.util.*"
 * 
 * Tests if the Deuce engine together with the pos transformation jvstm.EnhanceTransactional
 * change the super class from Object to VBox.
 * 
 * @author Fernando Miguel Carvalho
 */
@Exclude
public class AomMethodsTest {
	@Test
	public void testAomIntPointReplicate() throws Exception{
		/*
		 * Tests if the OBJECT_SIZE costanta is correctly initialized. 
		 */
		Field fSize = IntPoint.class.getDeclaredField("OBJECT_SIZE");
		Long size = (Long) fSize.get(null);
		Assert.assertEquals(32, size.intValue()); // For a 64 bits architecture
		/*
		 * Tests the replicate method. 
		 */	
		IntPoint x = new IntPoint(3, 7);
		Method mReplicate = IntPoint.class.getDeclaredMethod("replicate", null);
		IntPoint y = (IntPoint) mReplicate.invoke(x, null);
		Assert.assertEquals(x.x, y.x);
		Assert.assertEquals(x.y, y.y);
	}

	@Test
	public void testXptoClassReplicate() throws Exception{
		/*
		 * Tests if the OBJECT_SIZE costanta is correctly initialized. 
		 */
		Field fSize = Xpto.class.getDeclaredField("OBJECT_SIZE");
		Long size = (Long) fSize.get(null);
		Assert.assertEquals(48, size.intValue()); // For a 64 bits architecture
		/*
		 * Tests the replicate method. 
		 */	
		Xpto x = new Xpto((short) 15, 247, 551L, (byte) 9, "ole ola", 15.65);
		Method mReplicate = Xpto.class.getDeclaredMethod("replicate", null);
		Xpto y = (Xpto) mReplicate.invoke(x, null);
		Assert.assertEquals(x.a, y.a);
		Assert.assertEquals(x.b, y.b);
		Assert.assertEquals(x.c, y.c);
		Assert.assertEquals(x.d, y.d);
		Assert.assertEquals(x.e, y.e);
		Assert.assertEquals(x.f, y.f);
	}

	@Test
	public void testXptoClassToCompactLayout() throws Exception{
		/*
		 * Check the initial state 
		 */	
		Xpto x = new Xpto((short) 15, 247, 551L, (byte) 9, "ole ola", 15.65);
		Assert.assertEquals(15, x.a);
		Assert.assertEquals(247, x.b);
		Assert.assertEquals(551, x.c);
		Assert.assertEquals(9, x.d);
		Assert.assertEquals("ole ola", x.e);
		Assert.assertEquals(15.65, x.f);
		/*
		 * Creates a new Xpto instance corresponding to a new version and then 
		 * turns x to the compact layout. 
		 */	
		Xpto y = new Xpto((short) 47, 129, 428L, (byte) 14, "mane mana", 194.3);
		Method mToCompactLayout = Xpto.class.getDeclaredMethod("toCompactLayout", Xpto.class);
		mToCompactLayout.invoke(x, y);
		Assert.assertEquals(47, x.a);
		Assert.assertEquals(129, x.b);
		Assert.assertEquals(428, x.c);
		Assert.assertEquals(14, x.d);
		Assert.assertEquals("mane mana", x.e);
		Assert.assertEquals(194.3, x.f);

	}
	@Test
	public void testHashTableReplicate() throws Exception{
		Object x = new Hashtable();
		Hashtable y = ((VBox<Hashtable>) x).replicate();
		Field fTable =  Hashtable.class.getDeclaredField("table");
		Field fCount = Hashtable.class.getDeclaredField("count");
		Field fThreshold = Hashtable.class.getDeclaredField("threshold");
		Field fLoadFactor = Hashtable.class.getDeclaredField("loadFactor");
		Field fModCount = Hashtable.class.getDeclaredField("modCount");
		Field fKeySet = Hashtable.class.getDeclaredField("keySet");
		Field fEntrySet = Hashtable.class.getDeclaredField("entrySet");
		Field fValues = Hashtable.class.getDeclaredField("values");
		fTable.setAccessible(true);
		fCount.setAccessible(true);
		fThreshold.setAccessible(true);
		fLoadFactor.setAccessible(true);
		fModCount.setAccessible(true);
		fKeySet.setAccessible(true);
		fEntrySet.setAccessible(true);
		fValues.setAccessible(true);
		Assert.assertSame(fTable.get(x), fTable.get(y));
		Assert.assertEquals(fCount.get(x), fCount.get(y));
		Assert.assertEquals(fThreshold.get(x), fThreshold.get(y));
		Assert.assertEquals(fLoadFactor.get(x), fLoadFactor.get(y));
		Assert.assertEquals(fModCount.get(x), fModCount.get(y));
		Assert.assertSame(fKeySet.get(x), fKeySet.get(y));
		Assert.assertSame(fEntrySet.get(x), fEntrySet.get(y));
		Assert.assertSame(fValues.get(x), fValues.get(y));
	}

	@Test
	public void testHashTableToCompactLayout() throws Exception{
		/*
		 * SETUP 
		 */
		Hashtable x = new Hashtable();
		Field fTable =  Hashtable.class.getDeclaredField("table");
		Field fCount = Hashtable.class.getDeclaredField("count");
		Field fThreshold = Hashtable.class.getDeclaredField("threshold");
		Field fLoadFactor = Hashtable.class.getDeclaredField("loadFactor");
		Field fModCount = Hashtable.class.getDeclaredField("modCount");
		Field fKeySet = Hashtable.class.getDeclaredField("keySet");
		Field fEntrySet = Hashtable.class.getDeclaredField("entrySet");
		Field fValues = Hashtable.class.getDeclaredField("values");
		fTable.setAccessible(true);
		fCount.setAccessible(true);
		fThreshold.setAccessible(true);
		fLoadFactor.setAccessible(true);
		fModCount.setAccessible(true);
		fKeySet.setAccessible(true);
		fEntrySet.setAccessible(true);
		fValues.setAccessible(true);
		/*
		 * Check the initial state 
		 */	
		Assert.assertEquals(0, fCount.get(x));
		Assert.assertEquals(8, fThreshold.get(x));
		Assert.assertEquals(0.75f, ((Float)fLoadFactor.get(x)).floatValue());
		Assert.assertEquals(0, fModCount.get(x));
		/*
		 * Creates a new HashTable instance corresponding to a new version.
		 */	
		Hashtable y = new Hashtable(7, 0.4f);
		y.put(7, "matias");
		y.put(9, "josue");
		/*
		 * Turns x to the compact layout based on the previous y version 
		 */
		Method mToCompactLayout = Hashtable.class.getDeclaredMethod("toCompactLayout", Hashtable.class);
		mToCompactLayout.invoke(x, y);
		Assert.assertSame(fTable.get(y), fTable.get(x));
		Assert.assertEquals(2, fCount.get(x));
		Assert.assertEquals(2, fThreshold.get(x));
		Assert.assertEquals(0.4f, ((Float)fLoadFactor.get(x)).floatValue());
		Assert.assertEquals(2, fModCount.get(x));
		Assert.assertSame(fKeySet.get(x), fKeySet.get(x));
		Assert.assertSame(fEntrySet.get(x), fEntrySet.get(x));
		Assert.assertSame(fValues.get(x), fValues.get(x));
		/*
		 * Check elements 
		 */
		Assert.assertSame("matias", x.get(7));
		Assert.assertSame("josue", x.get(9));
	}
}
