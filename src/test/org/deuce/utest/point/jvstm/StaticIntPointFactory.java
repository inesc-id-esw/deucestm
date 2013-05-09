package org.deuce.utest.point.jvstm;

import org.deuce.utest.point.Point;
import org.deuce.utest.point.PointFactory;
import org.deuce.utest.point.PointTrxUtil;

public class StaticIntPointFactory implements PointFactory<Integer>{
    @Override
    public Point<Integer> make(Number paramInt1, Number paramInt2) {
	PointTrxUtil.staticPartToCompactLayout(StaticIntPoint.class);
	return new StaticIntPoint(paramInt1, paramInt2);
    }
}
