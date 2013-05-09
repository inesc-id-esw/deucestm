package org.deuce.utest.point.jvstm.runners;

import junit.framework.Assert;
import jvstm.Transaction;
import jvstm.VBox;

import org.deuce.Atomic;
import org.deuce.transform.Exclude;
import org.deuce.utest.point.Point;
import org.deuce.utest.point.PointFactory;
import org.deuce.utest.point.PointFields;
import org.junit.Test;


/**
 * This is an abstract class that defines the entry points for several unit tests 
 * that checks the correct functioning of the versioned boxes GC.
 * For each specialization of Point<T> we should provide the corresponding factory and
 * the unit test, which inherits from this class TestPoint<T> passing to the constructor
 * the concrete PointFactory<T> instance.
 * 
 * !!!! This test must run with the GC disabled:
 *   -Djvstm.aom.reversion.disabled=true
 * 
 * Run with the following configuration:
 * -javaagent:bin/deuceAgent.jar
 * -Dorg.deuce.transform.jvstm.trxClasses=org.deuce.utest.point.impl.*
 * -Dorg.deuce.transaction.contextClass=org.deuce.transaction.jvstm.Context
 * -Dorg.deuce.delegator=org.deuce.transaction.jvstm.ContextDelegator
 * -Dorg.deuce.transform.pos=org.deuce.transform.jvstm.EnhanceTransactional
 * -Dorg.deuce.exclude="sun.*,java.lang.*,jvstm.*,jvstm.gc.*,jvstm.util.*"
 * 
 * @author Fernando Miguel Carvalho
 */
@Exclude
public abstract class RunTestGcBodiesHistory<T extends Number>{

	protected final PointFactory<T> pointFac;
	protected final PointFields<T> fields;

	public RunTestGcBodiesHistory(PointFactory<T> pointFac, PointFields<T> fields) {
		this.pointFac = pointFac;
		this.fields = fields;
	}

	@Test
	public void testOneReversion(){
		int trxNumber = Transaction.mostRecentCommittedRecord.transactionNumber;
		// int nrOfTries =  VBox.nrOfTries;
		// int nrOfReversions =  VBox.nrOfReversions;
		Point<T> p = pointFac.make(7, 9);

		// The factory instantiates a Point object and the 
		// constructor does not invoke any barrier, so the
		// Point p must remain in the compact layout.	
		Assert.assertSame(null, ((VBox) p).body);

		TrxExecutorStatic.performTransaction(
				trxNumber, 
				0, //nrOfTries, 
				0, //nrOfReversions, 
				fields, 
				p, 
				getVBox(p) // get the head of the versions history
				);
	}

	protected abstract VBox<Point<T>> getVBox(Point<T> p);
}

class TrxExecutorStatic{
	@Atomic
	static <T extends Number> void incY(Point<T> p){
		/*
		 * The get/set properties perform STM barriers when invoked
		 * inside an atomic method. 
		 */
		p.setY(p.getY().intValue() + 1); 
	}

	@Atomic
	static <T extends Number> void incX(Point<T> p){
		p.setX(p.getX().intValue() + 1); // the get/set properties perform STM barriers. 
	}

	@Atomic
	static <T extends Number> long getX(Point<T> p){
		return p.getX().longValue();
	}

	@Atomic
	static <T extends Number> long getY(Point<T> p){
		return p.getY().longValue();
	}

	public static <T extends Number> void performTransaction(
			int trxNumber, 
			int nrOfTries, 
			int nrOfReversions, 
			PointFields<T> fields, 
			Point<T> p,
			VBox<Point<T>> vbox){
		// The following update is made inside an explicit transaction.
		incY(p);
		Assert.assertNotSame(null, vbox.body); // object extended 
		Assert.assertEquals(++trxNumber, vbox.body.version); // first version 2
		Assert.assertEquals(0, vbox.body.next.version); // the last one 0
		Assert.assertEquals(7, fields.getX(vbox.body.value).longValue()); // the body contains the original value
		Assert.assertEquals(7, p.getX().longValue()); // the field contains the original value
		Assert.assertEquals(10,fields.getY(vbox.body.value).longValue()); // the body contains the new value
		Assert.assertEquals(9, p.getY().longValue()); // the field contains the original value

		// The following update will create an Inevitable transaction
		// to perform the write operation in the Point coordinates and
		// it will be extended.
		incX(p); 
		Assert.assertNotSame(null, vbox.body);
		Assert.assertEquals(++trxNumber, vbox.body.version);
		Assert.assertEquals(trxNumber-1, vbox.body.next.version);
		Assert.assertEquals(0, vbox.body.next.next.version);
		Assert.assertEquals(8, fields.getX(vbox.body.value).longValue()); // the body contains the new value
		Assert.assertEquals(7, p.getX().longValue()); // the field contains the original value
		Assert.assertEquals(10,fields.getY(vbox.body.value).longValue()); // the body contains the new value
		Assert.assertEquals(9, p.getY().longValue()); // the field contains the original value


		// After running the GC the Point object should be reverted.
		// And the values of the most recent body will be copied to 
		// the standard fields of the Point object. 
		Transaction.gcTask.runGc();
		Assert.assertSame(null, vbox.body);
		Assert.assertEquals(8, getX(p)); // the STM barrier reads the object in-place 
		Assert.assertEquals(8, p.getX().longValue()); // the field contains the new value
		Assert.assertEquals(10, getY(p)); 
		Assert.assertEquals(10, p.getY().longValue());

		// Check the number of reversions
		// Assert.assertEquals(nrOfTries + 1, VBoxAom.nrOfTries);
		// Assert.assertEquals(nrOfReversions + 1, VBoxAom.nrOfReversions);
	}        
}
