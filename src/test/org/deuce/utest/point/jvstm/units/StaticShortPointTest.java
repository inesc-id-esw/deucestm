package org.deuce.utest.point.jvstm.units;

import org.deuce.utest.point.jvstm.StaticShortPointFactory;
import org.deuce.utest.point.jvstm.runners.RunAllTestPoint;


public class StaticShortPointTest extends RunAllTestPoint<Short>{

	public StaticShortPointTest() {

		// This field cannot be initialized via constructor because,
		// the JUnit does not allow more than one constructor.
		pointFac = new StaticShortPointFactory();
	}

}
