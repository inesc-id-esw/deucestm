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
import java.util.concurrent.ScheduledExecutorService;

import junit.framework.Assert;
import jvstm.SuspendedTransaction;
import jvstm.Transaction;

import org.deuce.transaction.TransactionException;
import org.deuce.utest.point.Point;

public class RunRwWithRw{
	private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);

	public static <T extends Number> void performTest(final Point<T> p) throws Exception{
		final long initX = p.getX().longValue(); 
		final long initY = p.getY().longValue();
		//
		// ThreadLocal -> rwTrx2
		//
		Transaction rwTrx2 = begin(false);
		long t2readX = getX(p).longValue();
		Assert.assertEquals(initX, t2readX);
		SuspendedTransaction r2Token =  suspendTx(rwTrx2);
		//
		// ThreadLocal -> rwTrx1 
		//
		Transaction rwTrx1 = begin(false);
		long t1readX = getX(p).longValue();
		Assert.assertEquals(initX, t1readX);
		SuspendedTransaction r1Token = suspendTx(rwTrx1);
		//
		// ThreadLocal -> rwTrx2
		//
		resume(rwTrx2, r2Token);
		long t2readY = getY(p).longValue();
		Assert.assertEquals(initY, t2readY);
		r2Token  = suspendTx(rwTrx2);
		//
		// ThreadLocal -> rwTrx1 
		//
		resume(rwTrx1, r1Token);
		long t1readY = getY(p).longValue();
		Assert.assertEquals(initY, t1readY);
		r1Token = suspendTx(rwTrx1);
		//
		// ThreadLocal -> rwTrx2
		//
		resume(rwTrx2, r2Token);
		setX(p, t2readX + 3);
		r2Token = suspendTx(rwTrx2);
		//
		// ThreadLocal -> rwTrx1 
		//
		resume(rwTrx1, r1Token );
		setX(p, t1readX + 7);
		r1Token = suspendTx(rwTrx1);
		//
		// ThreadLocal -> rwTrx2
		//
		resume(rwTrx2, r2Token);
		setY(p, t2readY - 3);
		r2Token  = suspendTx(rwTrx2);
		//
		// ThreadLocal -> rwTrx1 
		//
		resume(rwTrx1, r1Token);
		setY(p, t1readY - 7);
		r1Token = suspendTx(rwTrx1);
		//
		// ThreadLocal -> rwTrx2
		//
		resume(rwTrx2, r2Token );
		commit();
		//r2Token = suspendTx(rwTrx2);
		//
		// ThreadLocal -> rwTrx1 
		//
		resume(rwTrx1, r1Token );
		try{
			commit();
		}catch(TransactionException e){
			Transaction.abort();
			rwTrx1 = begin(false);
			t1readX = getX(p).longValue();
			Assert.assertEquals(initX + 3, t1readX);
			t1readY = getY(p).longValue();
			Assert.assertEquals(initY - 3, t1readY);
			setX(p, t1readY + 7);
			setY(p, t1readY - 7);
			commit();
			Assert.assertTrue(true);
			return;
		}
		Assert.assertTrue(false);
	}
}
