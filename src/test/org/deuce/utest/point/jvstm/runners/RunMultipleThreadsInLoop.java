package org.deuce.utest.point.jvstm.runners;

import java.util.Random;
import java.util.logging.Logger;

import junit.framework.Assert;
import jvstm.VBox;

import org.deuce.Atomic;
import org.deuce.transform.Exclude;
import org.deuce.utest.point.Point;


@Exclude
public class RunMultipleThreadsInLoop {

	final static private Logger logger = Logger.getLogger("org.deuce.utest");

	public static <T extends Number> void performTest(
			final int nrOfThreads, 
			final int nrOfIterations, 
			final Point<T> p ) throws InterruptedException{
		final Thread[] threads = new Thread[nrOfThreads];
		long coordsSum = p.getX().longValue() + p.getY().longValue();
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new WorkerThread<T>(nrOfIterations, p, coordsSum );
		}
		for (int i = 0; i < threads.length; i++) {
			threads[i].start();
		}
		for (int i = 0; i < threads.length; i++) {
			threads[i].join();
			logger.info(String.format("Thread %d release join!!!", threads[i].getId()));
		}
		// logger.info("Number of reversions = " + VBoxAom.nrOfReversions);
		// logger.info("Number of tries = " + VBoxAom.nrOfTries);
		logger.info("Object is " + ((VBox)p).body == null? "COMPACT" : "EXTENDED");
		long currX = Operations.readX(p);
		long currY = Operations.readY(p);
		long currSum = currX + currY;
		logger.info(String.format("%d + %d = %d", currX, currY, currSum));
		Assert.assertEquals("Final verification: ", coordsSum, currSum);
	}

	@Exclude
	static class WorkerThread<T extends Number> extends Thread{
		static final Random rand = new Random();

		final int nrOfIterations; 
		final Point<T> p;
		final long coordsSum;

		public WorkerThread(int nrOfIterations, Point<T> p, long coordsSum) {
			super();
			this.nrOfIterations = nrOfIterations;
			this.p = p;
			this.coordsSum = coordsSum;
		}

		@Override 
		public void run() {
			workerThread(nrOfIterations, p, coordsSum);
			logger.info(String.format("Thread %d finish!!!", Thread.currentThread().getId()));
		}

		public static <T extends Number> void workerThread(int nrOfIterations, Point<T> p, long coordsSum){
			for (int j = 0; j < nrOfIterations; j++) {
				try {
					final int idx = j;
					final String trxKind = "RW";
					String res = Operations.trx1(p, rand.nextInt(10) - 5, j%2);
					logger.finest(res);
					long currSum = Operations.trx2ro(p);
					Assert.assertEquals(String.format("iter: %d", idx), coordsSum, currSum);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	static class Operations{

		@Atomic
		public static <T extends Number> String trx1(Point<T> p, int valueToAdd, int odd){
			long x = p.getX().longValue();
			long y = p.getY().longValue();
			valueToAdd *= odd == 0? 1: -1;
			p.setX(x + valueToAdd);
			p.setY(y - valueToAdd);
			String res = String.format("%s: [x=%d, y=%d] ===> [x=%d, y=%d]",
					Thread.currentThread().getName(),
					x,
					y,
					x+valueToAdd,
					y-valueToAdd); 
			return res; 
		}
		@Atomic
		public static <T extends Number> long trx2ro(Point<T> p){
			// logger.info(String.format("iteration %d - %s - has read: %s", j, trxKind, res));
			long x = p.getX().longValue();
			long y = p.getY().longValue();
			return x + y;
		}

		/**
		 * Some STMS such as the JVSTM does NOT store transactional values
		 * in-place and in this case we must use always STM barriers to get 
		 * consistent values, even when we are out of the scope of any 
		 * transaction.
		 * But, once Deuce just use barriers inside atomic blocks, then we must 
		 * read fields inside an atomic block.
		 */
		@Atomic
		static <T extends Number> long readX(Point<T> p){
			return p.getX().longValue();
		}

		@Atomic
		static <T extends Number> long readY(Point<T> p){
			return p.getY().longValue();
		}

	}
}