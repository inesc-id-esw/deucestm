package org.deuce.utest.point;

public interface PointFactory<T extends Number>{
  Point<T> make(Number paramInt1, Number paramInt2);
}