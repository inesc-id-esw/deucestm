package org.deuce.utest.point.jvstm.runners;

import static org.deuce.utest.point.PointTrxUtil.begin;
import static org.deuce.utest.point.PointTrxUtil.commit;
import static org.deuce.utest.point.PointTrxUtil.getX;
import static org.deuce.utest.point.PointTrxUtil.getY;
import static org.deuce.utest.point.PointTrxUtil.resume;
import static org.deuce.utest.point.PointTrxUtil.setX;
import static org.deuce.utest.point.PointTrxUtil.setY;
import static org.deuce.utest.point.PointTrxUtil.suspendTx;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import jvstm.SuspendedTransaction;
import jvstm.Transaction;

import org.deuce.transaction.TransactionException;
import org.deuce.utest.point.Point;

public class RunRwWithRwConflictSameFields{
	private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);

	public static <T extends Number> void performTest(final Point<T> p) throws Exception{
		p.setX(13);
		p.setY(14);
		final long initX = p.getX().longValue(); 
		final long initY = p.getY().longValue();
		//
		// ThreadLocal -> rwTrx1 -> begin -> suspend
		//
		final Transaction rwTrx1 = begin(false);
		SuspendedTransaction r1Token = suspendTx(rwTrx1);
		//
		// ThreadLocal -> rwTrx2
		//
		final Transaction rwTrx2 = begin(false);
		Assert.assertEquals(initX, getX(p).longValue());
		Assert.assertEquals(initY, getY(p).longValue());
		setX(p, update(initX));
		setY(p, update(initY));
		Assert.assertEquals(update(initX), getX(p).longValue());
		Assert.assertEquals(update(initY), getY(p).longValue());
		final SuspendedTransaction r2Token = suspendTx(rwTrx2);
		//
		// ThreadLocal -> rwTrx1 -> this transaction will exchange 
		// the values of x and y. 
		//
		resume(rwTrx1, r1Token);
		long currX = getX(p).longValue();
		Assert.assertEquals(initX, currX);
		Assert.assertEquals(initY, getY(p).longValue());
		setX(p, initY);
		setY(p, initX);
		Assert.assertEquals(initY, getX(p).longValue());
		Assert.assertEquals(initX, getY(p).longValue());
		final SuspendedTransaction r1TokenB = suspendTx(rwTrx1);
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
		final Future<?> fT2 = executor.submit(new Runnable() { public void run() {
			resume(rwTrx2, r2Token);
			commit();
		}});
		//
		// ThreadLocal -> rwTrx1
		//
		Future<?> fT1 = executor.schedule(new Runnable() { public void run() {
			resume(rwTrx1, r1TokenB);
			try {
				commit();      
			} catch (TransactionException e) {
				Assert.assertTrue(true);
				rwTrx1.abortTx();
				//
				// ThreadLocal -> rwTrx1 -> restart a new transaction 
				// that will try to exchange the values of x and y. 
				//
				try {fT2.get();} 
				catch (Exception e2) {throw new RuntimeException(e2);}
				begin(false);
				long currX = getX(p).longValue();
				long currY = getY(p).longValue();
				Assert.assertEquals(update(initX), currX);
				Assert.assertEquals(update(initY), currY);
				setX(p, update(initY));
				setY(p, update(initX));
				Assert.assertEquals(update(initY), getX(p).longValue());
				Assert.assertEquals(update(initX), getY(p).longValue());
				commit();
				//
				// Main thread starts a new transaction
				//
				begin(true);
				Assert.assertEquals(update(initY), getX(p).longValue());
				Assert.assertEquals(update(initX), getY(p).longValue());
				commit();
				return;
			}
			Assert.assertTrue(false);
		}}, 1000, TimeUnit.MILLISECONDS);
		fT2.get();
		fT1.get();
		//
		// Main thread starts a new transaction
		//
		begin(true);
		currX = getX(p).longValue();
		currY = getY(p).longValue();
		Assert.assertEquals(update(initY), getX(p).longValue());
		Assert.assertEquals(update(initX), getY(p).longValue());
		commit();
	}
	private static long update(long src){
		return (src*4+6)/2;
	} 

}
