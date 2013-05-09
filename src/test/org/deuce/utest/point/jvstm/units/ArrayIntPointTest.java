package org.deuce.utest.point.jvstm.units;

import org.deuce.utest.point.jvstm.ArrayIntPointFactory;
import org.deuce.utest.point.jvstm.runners.RunAllTestPoint;


public class ArrayIntPointTest extends RunAllTestPoint<Integer>{

	public ArrayIntPointTest() {

		// This field cannot be initialized via constructor because,
		// the JUnit does not allow more than one constructor.
		pointFac = new ArrayIntPointFactory();
	}

}
