package org.deuce.utest.point.jvstm;

import org.deuce.transform.Exclude;
import org.deuce.utest.point.Point;
import org.deuce.utest.point.PointFactory;

@Exclude
public class IntPointFactory implements PointFactory<Integer>{

    @Override
    public Point<Integer> make(Number x, Number y) {
	return new IntPoint(x.intValue(), y.intValue());
    }

}
