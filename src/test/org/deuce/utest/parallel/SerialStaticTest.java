package org.deuce.utest.parallel;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.deuce.Atomic;

public class SerialStaticTest extends TestCase {
	
	private static int var0;
	private static int var1;
	private static int var2;
	private static int var3;
	private static int var4;
	
	/*
         * The JVSTM does not store transactional fields in place and 
         * therefore we should read through STM barriers to get 
         * consistent values. 
         */
        @Atomic static int var0(){return var0;}
        @Atomic static int var1(){return var1;}
        @Atomic static int var2(){return var2;}
        @Atomic static int var3(){return var3;}
        @Atomic static int var4(){return var4;}
        
    @Override
    public void setUp() { 
    	var0 = 0;
    	var1 = 1;
    	var2 = 2;
    	var3 = 3;
    	var4 = 4;
	}
	
	public void testSingleRead() {
		atomicSingleRead();
	}
	
	@Atomic
	private void atomicSingleRead() {
		int x = var1;
		Assert.assertEquals(1, var1);
	}

	public void testMultiRead() {
		AtomicMultiRead();
	}

	@Atomic
	private void AtomicMultiRead() {
		int x = var0;
		x += var1;
		x += var2;
		x += var3;
		x += var4;
	}

	
	public void testSingleWrite() {
		atomicSingleWrite();
		Assert.assertEquals(10, var0());
	}
	
	@Atomic
	public void atomicSingleWrite(){
		var0 = 10;	
	}
	
	public void testMultiWrite() {
		atmicMultiWrite();
	}

	@Atomic
	private void atmicMultiWrite() {
		var0 = 10;
		var1 = 10;
		var2 = 10;
		var3 = 10;
		var4 = 10;
	}

}
