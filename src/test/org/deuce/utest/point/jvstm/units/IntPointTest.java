package org.deuce.utest.point.jvstm.units;

import org.deuce.utest.point.jvstm.IntPointFactory;
import org.deuce.utest.point.jvstm.runners.RunAllTestPoint;


public class IntPointTest extends RunAllTestPoint<Integer>{

	public IntPointTest() {

		// This field cannot be initialized via constructor because,
		// the JUnit does not allow more than one constructor.
		pointFac = new IntPointFactory();
	}

}
