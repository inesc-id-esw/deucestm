package org.deuce.utest.point.jvstm;

import org.deuce.utest.point.Point;
import org.deuce.utest.point.PointFactory;
import org.deuce.utest.point.PointTrxUtil;

public class StaticLongPointFactory implements PointFactory<Long>{
    @Override
    public Point<Long> make(Number paramInt1, Number paramInt2) {
	PointTrxUtil.staticPartToCompactLayout(StaticLongPoint.class);
	return new StaticLongPoint(paramInt1.longValue(), paramInt2.longValue());
    }
}
