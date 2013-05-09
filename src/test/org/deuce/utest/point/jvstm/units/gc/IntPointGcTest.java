package org.deuce.utest.point.jvstm.units.gc;

import jvstm.VBox;

import org.deuce.transform.Exclude;
import org.deuce.utest.point.Point;
import org.deuce.utest.point.PointFields;
import org.deuce.utest.point.jvstm.IntPoint;
import org.deuce.utest.point.jvstm.IntPointFactory;
import org.deuce.utest.point.jvstm.runners.RunTestGcBodiesHistory;

/**
 * !!!! This test must run with the GC disabled:
 *   -Djvstm.aom.reversion.disabled=true
 */
@Exclude
public class IntPointGcTest extends RunTestGcBodiesHistory<Integer>{

	public IntPointGcTest() {
		super(new IntPointFactory(), new PointFields<Integer>(IntPoint.class));
	}

	/**
	 * In the JVSTM following the AOM approach the own transactional object
	 * corresponds to the VBox instance itself.  
	 */
	@Override
	protected VBox<Point<Integer>> getVBox(Point<Integer> p) {
		return (VBox<Point<Integer>>) p;
	}

}
