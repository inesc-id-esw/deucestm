package org.deuce.utest.point.jvstm;

import org.deuce.utest.point.Point;
import org.deuce.utest.point.PointFactory;


public class DoublePointFactory implements PointFactory<Double>{

    @Override
    public Point<Double> make(Number x, Number y) {
	return new DoublePoint(x.doubleValue(), y.doubleValue());
    }

}
