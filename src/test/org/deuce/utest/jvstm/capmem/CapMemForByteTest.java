package org.deuce.utest.jvstm.capmem;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.logging.Logger;

import junit.framework.Assert;
import jvstm.VBoxAom;

import org.deuce.Atomic;
import org.deuce.transform.Exclude;
import org.deuce.transform.jvstm.vboxes.VBoxByteArray;
import org.deuce.utest.capmem.AvoidBarriers;
import org.junit.Test;

/**
 * This test must be ran with the following System properties:
 * -javaagent:bin/deuceAgent.jar
 * -Dorg.deuce.delegator=org.deuce.transaction.jvstm.ContextDelegator
 * -Dorg.deuce.exclude=org.junit.*,org.deuce.utest.capmem.AvoidBarriers,java.lang.Integer,java.lang.reflect.*,jvstm.*
 *   
 * @author mcarvalho
 */
@Exclude
public class CapMemForByteTest {

	final static private Logger logger = Logger.getLogger("org.deuce.utest");

	@Test
	public void performTest() throws Exception{
		// Assert the top of classes hierarchy.
		Assert.assertSame(VBoxAom.class, A.class.getSuperclass());

		// Assert the additional syntethic fields containing the offsets 
		Field fX = A.class.getDeclaredField("x__ADDRESS__");
		Assert.assertEquals(20, fX.getLong(null));

		// Check the modification of array types 	
		Field fElements = A.class.getDeclaredField("elements");
		Assert.assertSame(VBoxByteArray.class, fElements.getType());

		// Perform a transaction in captured memory.
		A a = SimpleTrx.performTrxInCapturedMemory();
		Assert.assertEquals(4, a.x);
		Assert.assertEquals(7, a.y);

		// Perform a transaction.
		a = SimpleTrx.performTrx(new A());
		Assert.assertEquals(4, a.x());
		Assert.assertEquals(7, a.y());

		logger.info(a.toString());
	}

	private static class SimpleTrx{
		@Atomic
		public static A performTrx(A a){
			// Once the object A is not in captured memory then the following update
			// will perform a barrier and the new value stays in the write-set.
			a.x = 4;

			// The value in-place stays the same until the commit of the transaction. 
			byte n = AvoidBarriers.getByteValue(a, "x");
			Assert.assertEquals(0, n);

			// the same test for the array
			a.elements[0] = 11;
			Object elems = AvoidBarriers.getObjectValue(a.elements, "elements");
			Assert.assertEquals(1, Array.getByte(elems, 0));

			a.y = 7;
			return a;
		}

		@Atomic
		public static A performTrxInCapturedMemory(){
			A a = new A();
			// if the Object A is in captured memory then the following update 
			// will be in place and reading the field without an STM barrier 
			// must return the new value.
			a.x = 4;
			byte n = AvoidBarriers.getByteValue(a, "x");
			Assert.assertEquals(4, n);

			// the same test for the array
			a.elements[0] = 11;
			Object elems = AvoidBarriers.getObjectValue(a.elements, "elements");
			Assert.assertEquals(11, Array.getByte(elems, 0));

			a.y = 7;
			return a;
		}
	}

	static class A{
		public byte x, y;
		public byte[] elements;

		@Atomic public byte x(){return x;}
		@Atomic public byte y(){return y;}

		public A() {
			this.elements = new byte[3];
			this.elements[0] = 1;
		}


		@Override
		public String toString() {
			return "A [x=" + x + ", y=" + y + "]";
		}

	}
}
