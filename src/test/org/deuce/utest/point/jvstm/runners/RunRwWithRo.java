package org.deuce.utest.point.jvstm.runners;


import static org.deuce.utest.point.PointTrxUtil.begin;
import static org.deuce.utest.point.PointTrxUtil.commit;
import static org.deuce.utest.point.PointTrxUtil.getX;
import static org.deuce.utest.point.PointTrxUtil.getY;
import static org.deuce.utest.point.PointTrxUtil.resume;
import static org.deuce.utest.point.PointTrxUtil.setX;
import static org.deuce.utest.point.PointTrxUtil.setY;
import static org.deuce.utest.point.PointTrxUtil.suspendTx;
import junit.framework.Assert;
import jvstm.SuspendedTransaction;
import jvstm.Transaction;

import org.deuce.transform.Exclude;
import org.deuce.utest.point.Point;

@Exclude
public class RunRwWithRo{
	public static <T extends Number> void performTest(final Point<T> p) throws Exception{
		//
		// Main thread out of the scope of any transaction
		//
		final long initX = p.getX().longValue(); 
		final long initY = p.getY().longValue();
		//
		// ThreadLocal -> ro -> begin and suspend
		//
		Transaction roTrx = begin(true);
		SuspendedTransaction roTrxToken = suspendTx(roTrx);
		//
		// ThreadLocal -> rw
		//
		Transaction rwTrx = begin(false);
		Assert.assertEquals(initY, getY(p).longValue());
		Assert.assertEquals(initX, getX(p).longValue());
		setX(p, update(initX));
		setY(p, update(initY));
		Assert.assertEquals(update(initX), getX(p).longValue());
		Assert.assertEquals(update(initY), getY(p).longValue());
		SuspendedTransaction rwTrxToken =  suspendTx(rwTrx);
		//
		// ThreadLocal -> ro
		//
		resume(roTrx, roTrxToken);
		long currX = getX(p).longValue();
		Assert.assertEquals(initX, currX);
		Assert.assertEquals(initY, getY(p).longValue());
		roTrxToken = suspendTx(roTrx);
		//
		// Main thread out of the scope of any transaction
		//
		Assert.assertEquals(initX, p.getX().longValue());
		Assert.assertEquals(initY, p.getY().longValue());    
		//
		// ThreadLocal -> rw
		//
		resume(rwTrx, rwTrxToken);
		commit();
		//
		// ThreadLocal -> ro
		//
		resume(roTrx, roTrxToken );
		Assert.assertEquals(initX, getX(p).longValue());
		Assert.assertEquals(initY, getY(p).longValue());
		commit();
		//
		// A new transaction
		//
		begin(true);
		Assert.assertEquals(update(initX), getX(p).longValue());
		Assert.assertEquals(update(initY), getY(p).longValue());
		commit();
	}
	private static long update(long src){
		return (src*4+6)/2;
	} 

}
