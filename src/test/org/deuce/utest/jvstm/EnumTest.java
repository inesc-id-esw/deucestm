package org.deuce.utest.jvstm;

import junit.framework.TestCase;


public class EnumTest extends TestCase{

	public static void testEnums() throws Throwable{
		new EnumTestTarget().checkDirection(Direction.Ahead);
	}

	public static void testEnumsInAtomicScope() throws Exception{
		new EnumTestTarget().checkDirectionInAtomicScope(Direction.Ahead);
	}

	public static void main(String [] args)throws Throwable{
		testEnums();
	}
}
