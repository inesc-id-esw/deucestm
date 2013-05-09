package org.deuce.utest.point.jvstm;

import org.deuce.transform.Exclude;

import org.deuce.utest.point.Point;
import org.deuce.utest.point.PointFactory;

@Exclude
public class ArrayIntPointFactory implements PointFactory<Integer>{

    @Override
    public Point<Integer> make(Number x, Number y) {
	return new ArrayIntPoint(x.intValue(), y.intValue());
    }

}
