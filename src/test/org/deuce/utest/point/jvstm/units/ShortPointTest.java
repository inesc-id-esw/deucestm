package org.deuce.utest.point.jvstm.units;

import org.deuce.utest.point.jvstm.ShortPointFactory;
import org.deuce.utest.point.jvstm.runners.RunAllTestPoint;


public class ShortPointTest extends RunAllTestPoint<Short>{

	public ShortPointTest() {

		// This field cannot be initialized via constructor because,
		// the JUnit does not allow more than one constructor.
		pointFac = new ShortPointFactory();
	}

}
