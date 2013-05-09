package org.deuce.utest.point.jvstm;

import org.deuce.utest.point.Point;
import org.deuce.utest.point.PointFactory;

public class IntegerPointFactory implements PointFactory<Integer>{

    @Override
    public Point<Integer> make(Number x, Number y) {
	return new IntegerPoint(x.intValue(), y.intValue());
    }

}
