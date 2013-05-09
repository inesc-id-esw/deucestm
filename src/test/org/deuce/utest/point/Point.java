package org.deuce.utest.point;

public abstract interface Point<T extends Number>
{
  public abstract T getX();

  public abstract T getY();

  public abstract void setX(Number paramNumber);

  public abstract void setY(Number paramNumber);
}