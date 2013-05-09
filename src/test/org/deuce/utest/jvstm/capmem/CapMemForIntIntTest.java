package org.deuce.utest.jvstm.capmem;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

import junit.framework.Assert;
import jvstm.VBoxAom;

import org.deuce.Atomic;
import org.deuce.transform.Exclude;
import org.deuce.transform.jvstm.vboxes.VBoxIntArray;
import org.deuce.transform.jvstm.vboxes.VBoxObjectArray;
import org.deuce.utest.capmem.AvoidBarriers;
import org.junit.Test;

/**
 * This test must be ran with the following System properties:
 * -javaagent:bin/deuceAgent.jar
 * -Dorg.deuce.delegator=org.deuce.transaction.jvstm.ContextDelegator
 * -Dorg.deuce.exclude=org.junit.*,org.deuce.utest.capmem.AvoidBarriers,java.lang.Integer,java.lang.String,java.lang.reflect.*,sun.reflect.*,jvstm.*
 *   
 * @author mcarvalho
 */
@Exclude
public class CapMemForIntIntTest {

	@Test
	public void performTest() throws Exception{
		// Assert the top of classes hierarchy.
		Assert.assertSame(VBoxAom.class, A.class.getSuperclass());

		// Assert the additional syntethic fields containing the offsets 
		Field fX = A.class.getDeclaredField("x__ADDRESS__");
		Assert.assertEquals(20, fX.getLong(null));

		// Check the modification of array types 	
		Field fElements = A.class.getDeclaredField("elements");
		Assert.assertSame(VBoxObjectArray.class, fElements.getType());

		// Perform a transaction in captured memory.
		A a = SimpleTrx.performTrxInCapturedMemory();
		Assert.assertEquals(4, a.x);
		Assert.assertEquals(7, a.y);
	}

	@Test(expected=UnsupportedOperationException.class)
	public void performTestOnUnwrapp() throws Exception{
		// Perform a transaction.
		A a = SimpleTrx.performTrx(new A());
	}

	@Test(expected=UnsupportedOperationException.class)
	public void performTestOnUnwrapp2() throws Exception{
		// Perform a transaction.
		int[][] arr = SimpleTrx.performTrxCreatingArray();
	}

	@Test(expected=UnsupportedOperationException.class)
	public void performTestOnUnwrapp3() throws Exception{
		// Perform a transaction.
		int[][][] arr = SimpleTrx.performTrxCreatingArray3();
	}

	private static class SimpleTrx{

		@Atomic
		public static int[][] performTrxCreatingArray(){
			return new A().elements;
		}

		@Atomic
		public static int[][][] performTrxCreatingArray3(){
			return new C().elements;
		}

		@Atomic
		public static A performTrx(A a){
			// Once the object A is not in captured memory then the following update
			// will perform a barrier and the new value stays in the write-set.
			a.x = 4;

			// The value in-place stays the same until the commit of the transaction. 
			int n = AvoidBarriers.getIntValue(a, "x");
			Assert.assertEquals(0, n);

			// the same test for the array
			a.elements[0][0] = 11;
			Object wrapper = AvoidBarriers.getObjectValue(a.elements, "elements");
			VBoxIntArray wrapperInts =  (VBoxIntArray) Array.get(wrapper, 0);
			Object elems = AvoidBarriers.getObjectValue(wrapperInts, "elements");
			Assert.assertEquals(1, Array.getInt(elems, 0));

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
			int n = AvoidBarriers.getIntValue(a, "x");
			Assert.assertEquals(4, n);

			// the same test for the array
			a.elements[0][0] = 11;

			// a.elements is CapturedStateObjectArray -> CapturedStateIntArray[]
			Object wrapper = AvoidBarriers.getObjectValue(a.elements, "elements");
			VBoxIntArray wrapperInts =  (VBoxIntArray) Array.get(wrapper, 0);
			Object elems = AvoidBarriers.getObjectValue(wrapperInts, "elements");
			Assert.assertEquals(11, Array.getInt(elems, 0));

			a.y = 7;
			return a;
		}
	}

	private static class A{
		public int x, y;
		public int[][] elements;


		public A() {
			this.elements = new int[3][5];
			this.elements[0][0] = 1;
		}

		@Override
		public String toString() {
			return "A [x=" + x + ", y=" + y + "]";
		}
	}
	private static class C{
		public int[][][] elements;
		public C() {
			this.elements = new int[3][5][7];
			this.elements[0][0][0] = 1;
		}

	}
}
