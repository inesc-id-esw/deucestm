package org.deuce.utest.point.jvstm.units;

import org.deuce.utest.point.jvstm.StaticLongPointFactory;
import org.deuce.utest.point.jvstm.runners.RunAllTestPoint;


public class StaticLongPointTest extends RunAllTestPoint<Long>{

	public StaticLongPointTest() {

		// This field cannot be initialized via constructor because,
		// the JUnit does not allow more than one constructor.
		pointFac = new StaticLongPointFactory();
	}

}
