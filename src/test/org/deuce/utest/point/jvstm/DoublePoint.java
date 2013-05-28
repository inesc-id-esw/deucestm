package org.deuce.utest.point.jvstm;

import org.deuce.utest.point.Point;

public class DoublePoint implements Point<Double>{

    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *---------------------- FIELDS   ---------------------
     *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
    double x, y;

    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *----------------------  CTOR   ----------------------
     *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
    public DoublePoint(double x, double y) {
	this.x = x;
	this.y = y;
    }
    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *------------------ Point interface   ----------------
     *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
    @Override
    public Double getX() {
	return x;
    }

    @Override
    public Double getY() {
	return y;
    }

    @Override
    public void setX(Number x) {
	this.x = x.doubleValue();
    }

    @Override
    public void setY(Number y) {
	this.y = y.doubleValue();
    }
}
