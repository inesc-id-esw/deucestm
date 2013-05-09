package org.deuce.utest.point.jvstm.runners;

import junit.framework.TestCase;

import jvstm.Transaction;

import org.deuce.benchmark.AvailableProcs;
import org.deuce.transform.Exclude;
import org.deuce.utest.point.PointFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@Exclude
public abstract class RunAllTestPoint<T extends Number> extends TestCase{
	protected PointFactory<T> pointFac;

	@Before
	public void setUp(){
		if(Transaction.current() != null) 
			Transaction.abort();
		// Transaction.mostRecentCommittedRecord.clean(); ????? NullPointerExcpetion

		// Transaction.suspend();
		// Assert.assertEquals(0, TopLevelCounter.getRunning());
	} 
	@After
	public void tearDown(){
		// Assert.assertEquals(0, TopLevelCounter.getRunning());
	} 
	@Test
	public void testRunSingleThread() throws Exception{
		RunSingleThread.performTest(pointFac.make(7, 8));
	}
	@Test
	public void testRwWithRo() throws Exception{
		RunRwWithRo.performTest(pointFac.make(7, 8));
	}
	@Test
	public void testRunRwWithRw() throws Exception{
		RunRwWithRw.performTest(pointFac.make(7, 8));
	}
	@Test
	public void testRwWithRwConflictDisjointFields() throws Exception{
		RunRwWithRwConflictDisjointFields.performTest(pointFac.make(7, 8));
	}
	@Test
	public void testRwWithRwConflictSameFields() throws Exception{
		RunRwWithRwConflictSameFields.performTest(pointFac.make(7, 8));
	}
	@Test
	public void testRwWithRwNoWaitAllExtendedObjects() throws Exception{
		RunRwWithRwNoWaitAllExtendedObjects.performTest(pointFac.make(7, 8));
	}
	@Test
	public void testRunMultipleThreadsInLoop() throws Exception{
		RunMultipleThreadsInLoop.performTest(
				Runtime.getRuntime().availableProcessors(), 1024*16, pointFac.make(7, 8));
	}
}
