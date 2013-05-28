package org.deuce.utest.point.jvstm;

import org.deuce.utest.point.Point;

public class IntPoint implements Point<Integer>{
	/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	 *---------------------- FIELDS   ---------------------
	 *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
	public int x, y;

	/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	 *----------------------  CTOR   ----------------------
	 *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
	public IntPoint(int x, int y) {
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
		this.x = x.intValue();
	}

	@Override
	public void setY(Number y) {
		this.y = y.intValue();
	}
	@Override
	public String toString() {
		return "[x=" + x + ", y=" + y + "]";
	}
}
