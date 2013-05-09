package org.deuce.utest.point.jvstm;

import org.deuce.utest.point.Point;

public class StaticLongPoint implements Point<Long> {
    static long x;
    static long y;
    
    public StaticLongPoint()
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
    public StaticLongPoint(long paramInt1, long paramInt2)
    {
	StaticLongPoint.x = paramInt1; 
	StaticLongPoint.y = paramInt2;
	//TrxUtil.printStaticUnifiedField(StaticIntPoint.class, "x");
	//TrxUtil.printStaticUnifiedField(StaticIntPoint.class, "y");
    }
    
    public Long getX()
    {
	// The following code will be instrumented by TrfClassUnifyFields
	// replacing it by something like:
	// return Integer.valueOf(StaticIntPoint.STATIC_PART$.x); 	
      return StaticLongPoint.x;
    }

    public Long getY()
    {
      return StaticLongPoint.y;
    }

    public void setX(Number paramNumber)
    {
	StaticLongPoint.x = paramNumber.longValue();
    }

    public void setY(Number paramNumber)
    {
	StaticLongPoint.y = paramNumber.longValue();
    }
}