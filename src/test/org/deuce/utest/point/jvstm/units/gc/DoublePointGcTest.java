package org.deuce.utest.point.jvstm.units.gc;

import jvstm.VBox;

import org.deuce.transform.Exclude;
import org.deuce.utest.point.Point;
import org.deuce.utest.point.PointFields;
import org.deuce.utest.point.jvstm.DoublePoint;
import org.deuce.utest.point.jvstm.DoublePointFactory;
import org.deuce.utest.point.jvstm.runners.RunTestGcBodiesHistory;

/**
 * !!!! This test must run with the GC disabled:
 *   -Djvstm.aom.gc.disabled=true
 */
@Exclude
public class DoublePointGcTest extends RunTestGcBodiesHistory<Double>{

	public DoublePointGcTest() {
		super(new DoublePointFactory(), new PointFields<Double>(DoublePoint.class));
	}


	/**
	 * In the JVSTM following the AOM approach the own transactional object
	 * corresponds to the VBox instance itself.  
	 */
	@Override
	protected VBox<Point<Double>> getVBox(Point<Double> p) {
		return (VBox<Point<Double>>) p;
	}
}
