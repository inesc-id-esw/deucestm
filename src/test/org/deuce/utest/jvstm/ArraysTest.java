package org.deuce.utest.jvstm;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

import junit.framework.Assert;

import org.deuce.Atomic;
import org.deuce.transform.Exclude;
import org.deuce.transform.jvstm.vboxes.VBoxIntArray;
import org.junit.Test;

@Exclude
public class ArraysTest {

	public static void main(String [] args)throws Exception{
		new ArraysTest().performTestOnarrays();
	}

	@Test
	public void performTestOnarrays(){
		int[] a = ArraysTestTarget.makeArray(5);
		Assert.assertEquals(5, a.length);
	}
}

class ArrayStorage{
	int[] elems;

	public ArrayStorage(int size) {
		this.elems = new int[size];
	}

	@Atomic
	public int get(int index){
		return elems[index];
	}
}

class ArraysTestTarget{
	public static int[] makeArray(int length){
		ArrayStorage arr = new ArrayStorage(7);
		for (int i = 0; i < arr.elems.length; i++) {
			Assert.assertEquals(0, arr.elems[i]);
		}
		int[] res = makeTransactionalArray(length, arr);
		Assert.assertEquals(13, arr.get(0));
		Assert.assertEquals(17, arr.get(1));
		return res;
	}

	@Atomic
	private static int[] makeTransactionalArray(int length, ArrayStorage arr){
		int[] res = new int[length];
		checkVBoxArray(res, length);

		arr.elems[0] = 13;
		arr.elems[1] = 17;
		return res;
	}

	public static void checkVBoxArray(Object o, int length){
		try {
			VBoxIntArray vbox = (VBoxIntArray) o;
			Field fElements = VBoxIntArray.class.getDeclaredField("elements");
			Object elems = fElements.get(o);
			Assert.assertEquals(length, Array.getLength(elems));
		} 
		catch (NoSuchFieldException e) {throw new RuntimeException(e);} 
		catch (SecurityException e) {throw new RuntimeException(e);} 
		catch (IllegalArgumentException e) {throw new RuntimeException(e);} 
		catch (IllegalAccessException e) {throw new RuntimeException(e);} 
	}
}