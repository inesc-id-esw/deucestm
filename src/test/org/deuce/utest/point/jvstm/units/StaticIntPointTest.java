package org.deuce.utest.point.jvstm.units;

import org.deuce.utest.point.jvstm.StaticIntPointFactory;
import org.deuce.utest.point.jvstm.runners.RunAllTestPoint;


public class StaticIntPointTest extends RunAllTestPoint<Integer>{

	public StaticIntPointTest() {

		// This field cannot be initialized via constructor because,
		// the JUnit does not allow more than one constructor.
		pointFac = new StaticIntPointFactory();
	}

}
