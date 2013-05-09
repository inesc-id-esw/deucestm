package org.deuce.utest.jvstm.capmem;

import junit.framework.Assert;

import org.deuce.transform.Exclude;
import org.deuce.transform.jvstm.vboxes.VBoxIntArray;
import org.deuce.transform.jvstm.vboxes.VBoxObjectArray;
import org.junit.Test;

@Exclude
public class MultiArrayTest {
	@Test
	public void performTestBiDim() throws Exception{
		int[][] elems = {{1,2,3},{4,5,6},{7,8,9}};
		org.deuce.transaction.jvstm.Context ctx = new org.deuce.transaction.jvstm.Context();
		ctx.init(1, "");
		VBoxObjectArray wrapper = new VBoxObjectArray(elems, ctx.currentTrx);
		for (int i = 0; i < elems.length; i++) {
			Assert.assertSame(VBoxIntArray.class, wrapper.elements[i].getClass());

			// The inner array must be equals to its original
			Assert.assertSame(elems[i], ((VBoxIntArray)wrapper.elements[i]).elements);	    
		}
		// Now unwrap
		int[][] newElems = (int[][]) wrapper.unwrapp();
		for (int i = 0; i < elems.length; i++) {
			Assert.assertSame(elems[i], newElems[i]);	    
		}
		ctx.commit();
	}

	@Test
	public void performTestBiDimObject() throws Exception{
		String[][] elems = {{"1","2","3"},{"4","5","6"},{"7","8","9"}};
		org.deuce.transaction.jvstm.Context ctx = new org.deuce.transaction.jvstm.Context();
		ctx.init(1, "");
		VBoxObjectArray wrapper = new VBoxObjectArray(elems, ctx.currentTrx);
		for (int i = 0; i < elems.length; i++) {
			Assert.assertSame(VBoxObjectArray.class, wrapper.elements[i].getClass());

			// The inner array must be equals to its original
			Assert.assertSame(elems[i], ((VBoxObjectArray)wrapper.elements[i]).elements);	    
		}
		// Now unwrap
		String[][] newElems = (String[][]) wrapper.unwrapp();
		for (int i = 0; i < elems.length; i++) {
			for (int j = 0; j < elems[i].length; j++) {
				Assert.assertEquals(elems[i][j], newElems[i][j]);
			}	    
		}
		ctx.commit();
	}

	@Test
	public void performTestTriDim()throws Exception{
		int[][][] elems = new int[3][7][5];
		org.deuce.transaction.jvstm.Context ctx = new org.deuce.transaction.jvstm.Context();
		ctx.init(1, "");
		VBoxObjectArray wrapper = new VBoxObjectArray(elems, ctx.currentTrx);
		for (int i = 0; i < elems.length; i++) {
			// Outer wrapper is CapturedStateObjectArray
			Assert.assertSame(VBoxObjectArray.class, wrapper.elements[i].getClass());

			// Inner wrapper is CapturedStateIntArray
			VBoxObjectArray inner = (VBoxObjectArray) wrapper.elements[i];
			for (int j = 0; j < inner.elements.length; j++) {
				Assert.assertSame(VBoxIntArray.class, inner.elements[j].getClass());
				VBoxIntArray innerinner = (VBoxIntArray) inner.elements[j];
				Assert.assertSame(elems[i][j], innerinner.elements);
			}
		}

		// Now unwrap
		int[][][] newElems = (int[][][]) wrapper.unwrapp();
		for (int i = 0; i < elems.length; i++) {
			for (int j = 0; j < elems[i].length; j++) {
				Assert.assertSame(elems[i][j], newElems[i][j]);
			}	    
		}
		ctx.commit();
	}

	@Test
	public void performTestTriDimObject()throws Exception{
		String[][][] elems = {
				{{"1", "4"},{"7", "10"},{"A", "d"}},
				{{"2", "5"},{"8", "11"},{"B", "e"}},
				{{"3", "6"},{"9", "12"},{"C", "f"}}};
		org.deuce.transaction.jvstm.Context ctx = new org.deuce.transaction.jvstm.Context();
		ctx.init(1, "");
		VBoxObjectArray wrapper = new VBoxObjectArray(elems, ctx.currentTrx);
		for (int i = 0; i < elems.length; i++) {
			// Outer wrapper is CapturedStateObjectArray
			Assert.assertSame(VBoxObjectArray.class, wrapper.elements[i].getClass());

			// Inner wrapper is CapturedStateIntArray
			VBoxObjectArray inner = (VBoxObjectArray) wrapper.elements[i];
			for (int j = 0; j < inner.elements.length; j++) {
				Assert.assertSame(VBoxObjectArray.class, inner.elements[j].getClass());
				VBoxObjectArray innerinner = (VBoxObjectArray) inner.elements[j];
				Assert.assertSame(elems[i][j], innerinner.elements);
			}
		}
		// Now unwrap
		String[][][] newElems = (String[][][]) wrapper.unwrapp();
		for (int i = 0; i < elems.length; i++) {
			for (int j = 0; j < elems[i].length; j++) {
				for (int k = 0; k < newElems[i][j].length; k++) {
					Assert.assertEquals(elems[i][j][k], newElems[i][j][k]);   
				}
			}	    
		}
		ctx.commit();
	}
}
