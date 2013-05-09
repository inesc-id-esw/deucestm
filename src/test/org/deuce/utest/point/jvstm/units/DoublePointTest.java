package org.deuce.utest.point.jvstm.units;

import org.deuce.utest.point.jvstm.DoublePointFactory;
import org.deuce.utest.point.jvstm.runners.RunAllTestPoint;


public class DoublePointTest extends RunAllTestPoint<Double>{

	public DoublePointTest() {

		// This field cannot be initialized via constructor because,
		// the JUnit does not allow more than one constructor.
		pointFac = new DoublePointFactory();
	}
}
