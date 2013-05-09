package org.deuce.utest.point.jvstm.units.gc;

import static org.deuce.utest.point.PointTrxUtil.begin;
import static org.deuce.utest.point.PointTrxUtil.setX;
import static org.deuce.utest.point.PointTrxUtil.setY;
import junit.framework.Assert;
import jvstm.Transaction;

import org.deuce.Atomic;
import org.deuce.transform.Exclude;
import org.deuce.utest.point.Point;
import org.deuce.utest.point.PointFactory;
import org.deuce.utest.point.PointTrxUtil;
import org.deuce.utest.point.jvstm.IntPointFactory;
import org.deuce.utest.point.jvstm.StaticIntPoint;
import org.deuce.utest.point.jvstm.StaticIntPointFactory;
import org.deuce.utest.point.jvstm.StaticLongPoint;
import org.deuce.utest.point.jvstm.StaticLongPointFactory;
import org.junit.Test;

/**
 * Some STMS such as the JVSTM does NOT store transactional values
 * in-place and in this case we must use always STM barriers to get 
 * consistent values, even when we are out of the scope of any 
 * transaction.
 * But, once Deuce just use barriers inside atomic blocks, then we must 
 * read fields inside an atomic block.
 * Note that we do not put all the assertion inside here to avoid its 
 * transactification.      
 * 
 */
class PointAccessors{
	@Atomic
	static <T extends Number> int getX(Point<T> p){
		return p.getX().intValue();
	}
	@Atomic
	static <T extends Number> int getY(Point<T> p){
		return p.getY().intValue();
	}
}

@Exclude
public class AppGcForStaticFields {
	public static void main(String [] args){
		AppGcForStaticFields app = new AppGcForStaticFields();
		app.testStaticLongPoint();
		app.testStaticIntPoint();
		app.testIntPoint();
	}

	private static void updatePoint(Point<? extends Number> p, int x, int y){
		begin(false);
		setX(p, x);
		setY(p, y);
		PointTrxUtil.commit();
	}

	@Test
	public <T extends Number> void testStaticLongPoint(){
		testStaticPoint(new StaticLongPointFactory());
	}

	@Test
	public <T extends Number> void testStaticIntPoint(){
		testStaticPoint(new StaticIntPointFactory());
	}

	static <T extends Number> void testStaticPoint(PointFactory<T> fac){
		//
		// Init
		//
		Point<T> p = fac.make(8, 9);
		Assert.assertEquals(8, PointAccessors.getX(p));
		Assert.assertEquals(9, PointAccessors.getY(p));
		PointTrxUtil.staticPartPrintHistory(p.getClass());
		//
		// trx 1
		//	
		updatePoint(p, 7, 8);
		Assert.assertEquals(7, PointAccessors.getX(p));
		Assert.assertEquals(8, PointAccessors.getY(p));
		PointTrxUtil.staticPartPrintHistory(p.getClass());
		Transaction.gcTask.runGc();
		PointTrxUtil.staticPartPrintHistory(p.getClass());
		//
		// trx 2
		//      
		updatePoint(p, 11, 5);
		Assert.assertEquals(11, PointAccessors.getX(p));
		Assert.assertEquals(5, PointAccessors.getY(p));
		PointTrxUtil.staticPartPrintHistory(p.getClass());      
		Transaction.gcTask.runGc();
		PointTrxUtil.staticPartPrintHistory(p.getClass());

	}

	@Test
	public void testIntPoint(){
		//
		// Init
		//
		Point<Integer> p = new IntPointFactory().make(8, 9);
		PointTrxUtil.printHistory(p);
		//
		// trx 1
		//	
		updatePoint(p, 7, 8);
		PointTrxUtil.printHistory(p);
		//
		// trx 2
		//	
		updatePoint(p, 11, 5);
		PointTrxUtil.printHistory(p);
		Transaction.gcTask.runGc();
		PointTrxUtil.printHistory(p);	

	}
}
