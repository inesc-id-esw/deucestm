package org.deuce.utest.point.jvstm.units;

import org.deuce.utest.point.jvstm.StaticIntegerPointFactory;
import org.deuce.utest.point.jvstm.runners.RunAllTestPoint;


public class StaticIntegerPointTest extends RunAllTestPoint<Integer>{

	public StaticIntegerPointTest() {

		// This field cannot be initialized via constructor because,
		// the JUnit does not allow more than one constructor.
		pointFac = new StaticIntegerPointFactory();
	}

}
