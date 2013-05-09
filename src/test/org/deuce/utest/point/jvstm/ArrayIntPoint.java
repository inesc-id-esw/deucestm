package org.deuce.utest.point.jvstm;

import org.deuce.utest.point.Point;

import jvstm.AomBarriers;


import jvstm.ReadWriteTransaction;
import jvstm.Transaction;
import jvstm.UtilUnsafe;

public class ArrayIntPoint implements Point<Integer>{
    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *---------------------- FIELDS   ---------------------
     *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
    public int [] coords; // index 0 for x and index 1 for y 

    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *----------------------  CTOR   ----------------------
     *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
    public ArrayIntPoint(int x, int y) {
	this.coords = new int[2];
	this.coords[0] = x;
	this.coords[1] = y;
    }
    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *------------------ Point interface   ----------------
     *~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
    @Override
    public Integer getX() {
	return this.coords[0];
    }

    @Override
    public Integer getY() {
	return this.coords[1];
    }

    @Override
    public void setX(Number x) {
        this.coords[0] = x.intValue();
    }

    @Override
    public void setY(Number y) {
        this.coords[1] = y.intValue();
    }
    @Override
    public String toString() {
        return "[x=" + this.coords[0] + ", y=" + this.coords[1] + "]";
    }
}
