package org.deuce.utest.point.jvstm;

import org.deuce.utest.point.Point;
import org.deuce.utest.point.PointFactory;
import org.deuce.utest.point.PointTrxUtil;


public class StaticShortPointFactory implements PointFactory<Short>{
    @Override
    public Point<Short> make(Number paramInt1, Number paramInt2) {
	PointTrxUtil.staticPartToCompactLayout(StaticShortPoint.class);
	return new StaticShortPoint(paramInt1.shortValue(), paramInt2.shortValue());
    }
}
