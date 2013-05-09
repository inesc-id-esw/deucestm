package org.deuce.utest.point.jvstm;

import org.deuce.utest.point.Point;

public class StaticShortPoint implements Point<Short> {
    static short x;
    static short y;
    
    public StaticShortPoint()
    {
    }
    
    /**
     * The transactional version of this constructor, which receives a 3rd argument
     * Context, will replace the following statements by:
     * ContextDelegator.onWriteAccess(
     * 		StaticIntPoint.STATIC_PART$, 
     * 		paramInt1, 
     * 		StaticIntPoint__STATICFIELDS__DeuceFieldsHolder,
     * 		Context)
     */
    public StaticShortPoint(short paramInt1, short paramInt2)
    {
	StaticShortPoint.x = paramInt1; 
	StaticShortPoint.y = paramInt2;
	//TrxUtil.printStaticUnifiedField(StaticIntPoint.class, "x");
	//TrxUtil.printStaticUnifiedField(StaticIntPoint.class, "y");
    }
    
    public Short getX()
    {
	// The following code will be instrumented by TrfClassUnifyFields
	// replacing it by something like:
	// return Integer.valueOf(StaticIntPoint.STATIC_PART$.x); 	
      return StaticShortPoint.x;
    }

    public Short getY()
    {
      return StaticShortPoint.y;
    }

    public void setX(Number paramNumber)
    {
	StaticShortPoint.x = paramNumber.shortValue();
    }

    public void setY(Number paramNumber)
    {
	StaticShortPoint.y = paramNumber.shortValue();
    }
}