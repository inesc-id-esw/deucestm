package org.deuce.utest.point.jvstm;

import org.deuce.utest.point.Point;

public class StaticIntegerPoint implements Point<Integer> {
    static int x;
    static int y;
    int z;
    
    public StaticIntegerPoint()
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
    public StaticIntegerPoint(Number paramInt1, Number paramInt2)
    {
	StaticIntegerPoint.x = paramInt1.intValue(); 
	StaticIntegerPoint.y = paramInt2.intValue();
	//TrxUtil.printStaticUnifiedField(StaticIntPoint.class, "x");
	//TrxUtil.printStaticUnifiedField(StaticIntPoint.class, "y");
    }
    
    public Integer getX()
    {
	// The following code will be instrumented by TrfClassUnifyFields
	// replacing it by something like:
	// return Integer.valueOf(StaticIntPoint.STATIC_PART$.x); 	
      return Integer.valueOf(StaticIntegerPoint.x);
    }

    public Integer getY()
    {
      return Integer.valueOf(StaticIntegerPoint.y);
    }

    public void setX(Number paramNumber)
    {
	StaticIntegerPoint.x = paramNumber.intValue();
    }

    public void setY(Number paramNumber)
    {
	StaticIntegerPoint.y = paramNumber.intValue();
    }
}