package org.deuce.utest.point.jvstm;

import org.deuce.utest.point.Point;


public class IntegerPoint implements Point<Integer>{
	/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	 *---------------------- FIELDS   ---------------------
	 *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
	Integer x, y;

	/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	 *----------------------  CTOR   ----------------------
	 *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
	public IntegerPoint(Integer x, Integer y) {
		this.x = x;
		this.y = y;
	}
	/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	 *------------------ Point interface   ----------------
	 *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
	@Override
	public Integer getX() {
		return x;
	}

	@Override
	public Integer getY() {
		return y;
	}

	@Override
	public void setX(Number x) {
		this.x = new Integer(x.intValue());
	}

	@Override
	public void setY(Number y) {
		this.y = new Integer(y.intValue());
	}
}
