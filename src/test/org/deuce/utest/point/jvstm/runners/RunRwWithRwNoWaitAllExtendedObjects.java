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

import org.deuce.transaction.TransactionException;
import org.deuce.utest.point.Point;


public class RunRwWithRwNoWaitAllExtendedObjects{
	public static <T extends Number> void performTest(final Point<T> p) throws Exception{
		p.setX(13);
		p.setY(14);
		final long initX = p.getX().longValue(); 
		final long initY = p.getY().longValue();
		//
		// ThreadLocal -> rwTrx1 -> begin and suspend 
		//
		final Transaction rwTrx1 = begin(false);
		SuspendedTransaction r1Token = suspendTx(rwTrx1);
		//
		// ThreadLocal -> rwTrx2
		//
		final Transaction rwTrx2 = begin(false);
		Assert.assertEquals(initY, getY(p).longValue());
		setY(p, update(initY));
		Assert.assertEquals(update(initY), getY(p).longValue());
		SuspendedTransaction r2Token = suspendTx(rwTrx2);
		//
		// ThreadLocal -> rwTrx1 
		//
		resume(rwTrx1, r1Token);
		long currX = getX(p).longValue();
		Assert.assertEquals(initX, currX);
		setX(p, update(initX));
		Assert.assertEquals(update(initX), getX(p).longValue());
		r1Token = suspendTx(rwTrx1);
		//
		// Main thread starts a new transaction
		//
		begin(true);
		currX = getX(p).longValue();
		long currY = getY(p).longValue();
		Assert.assertEquals(initX, currX);
		Assert.assertEquals(initY, currY);
		commit();
		//
		// ThreadLocal -> rwTrx2
		//
		resume(rwTrx2, r2Token);
		commit();
		//
		// Main thread starts a new transaction
		//
		begin(true);
		currX = getX(p).longValue();
		currY = getY(p).longValue();
		Assert.assertEquals(initX, currX);
		Assert.assertEquals(update(initY), currY);
		commit();
		//
		// ThreadLocal -> rwTrx1
		//
		resume(rwTrx1, r1Token);
		currX = getX(p).longValue();
		Assert.assertEquals(update(initX), currX);
		try{
			commit(); // The commit succeeds with conflict detection granularity of word-level 
			Assert.assertTrue(false); // Fields with different VBoxes will not conflict
		}catch(TransactionException e){
			Assert.assertTrue(true); 
			rwTrx1.abortTx();
		}
		//
		// Main thread starts a new transaction
		//
		begin(true);
		Assert.assertEquals(initX, getX(p).longValue());
		Assert.assertEquals(update(initY), getY(p).longValue());
		commit();
	}
	private static long update(long src){
		return (src*4+6)/2;
	} 

}
