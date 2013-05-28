package org.deuce.utest.point.jvstm;

import org.deuce.utest.point.Point;
import org.deuce.utest.point.PointFactory;


public class ShortPointFactory implements PointFactory<Short>{

    @Override
    public Point<Short> make(Number x, Number y) {
	return new ShortPoint(x.shortValue(), y.shortValue());
    }

}
