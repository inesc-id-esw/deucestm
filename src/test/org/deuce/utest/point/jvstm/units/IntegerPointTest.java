package org.deuce.utest.point.jvstm.units;

import org.deuce.utest.point.jvstm.IntegerPointFactory;
import org.deuce.utest.point.jvstm.runners.RunAllTestPoint;


public class IntegerPointTest extends RunAllTestPoint<Integer>{

	public IntegerPointTest() {

		// This field cannot be initialized via constructor because,
		// the JUnit does not allow more than one constructor.
		pointFac = new IntegerPointFactory();
	}

}
