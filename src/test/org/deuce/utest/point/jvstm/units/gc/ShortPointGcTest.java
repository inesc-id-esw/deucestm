package org.deuce.utest.point.jvstm.units.gc;

import jvstm.VBox;

import org.deuce.transform.Exclude;
import org.deuce.utest.point.Point;
import org.deuce.utest.point.PointFields;
import org.deuce.utest.point.jvstm.ShortPoint;
import org.deuce.utest.point.jvstm.ShortPointFactory;
import org.deuce.utest.point.jvstm.runners.RunTestGcBodiesHistory;

/**
 * !!!! This test must run with the GC disabled:
 *   -Djvstm.aom.reversion.disabled=true
 */
@Exclude
public class ShortPointGcTest extends RunTestGcBodiesHistory<Short>{

	public ShortPointGcTest() {
		super(new ShortPointFactory(), new PointFields<Short>(ShortPoint.class));
	}

	/**
	 * In the JVSTM following the AOM approach the own transactional object
	 * corresponds to the VBox instance itself.  
	 */
	@Override
	protected VBox<Point<Short>> getVBox(Point<Short> p) {
		return (VBox<Point<Short>>) p;
	}

}
