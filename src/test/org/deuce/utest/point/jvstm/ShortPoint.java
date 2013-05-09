package org.deuce.utest.point.jvstm;

import org.deuce.utest.point.Point;


import jvstm.AomBarriers;

public class ShortPoint implements Point<Short>{
    
    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *---------------------- FIELDS   ---------------------
     *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
    short x, y;

    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *----------------------  CTOR   ----------------------
     *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
    public ShortPoint(short x, short y) {
	this.x = x;
	this.y = y;
    }
    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *------------------ Point interface   ----------------
     *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
    @Override
    public Short getX() {
	return x;
    }

    @Override
    public Short getY() {
	return y;
    }

    @Override
    public void setX(Number x) {
	this.x = x.shortValue(); 
    }

    @Override
    public void setY(Number y) {
	this.y = y.shortValue();
    }
}
