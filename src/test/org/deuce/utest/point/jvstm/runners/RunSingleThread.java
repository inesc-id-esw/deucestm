package org.deuce.utest.point.jvstm.runners;

import java.util.concurrent.Callable;

import junit.framework.Assert;
import jvstm.Transaction;

import org.deuce.utest.point.Point;

public class RunSingleThread{
	public static <T extends Number> void performTest(final Point<T> p) throws Exception{
		final long initX = p.getX().longValue(); 
		final long initY = p.getY().longValue();
		Transaction.doIt(
				new Callable<Integer>(){
					public Integer call()throws Exception {
						return updateXyOnAtomicPoint(p);
					}
				}
				);
		Assert.assertEquals(update(initX), p.getX().longValue());
		Assert.assertEquals(update(initY), p.getY().longValue());
	}
	private static <T extends Number> int updateXyOnAtomicPoint(final Point<T> p){
		final long initX = p.getX().longValue(); 
		final long initY = p.getY().longValue();
		p.setX(update(initX));
		p.setY(update(initY));    
		return 0;
	}
	private static long update(long src){
		return (src*4+6)/2;
	} 
}
