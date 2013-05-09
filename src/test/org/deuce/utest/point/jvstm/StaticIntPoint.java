package org.deuce.utest.point.jvstm;

import org.deuce.utest.point.Point;

public class StaticIntPoint implements Point<Integer> {
    static Integer x;
    static Integer y;
    Integer z;
    
    public StaticIntPoint()
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
    public StaticIntPoint(Number paramInt1, Number paramInt2)
    {
	StaticIntPoint.x = paramInt1.intValue(); 
	StaticIntPoint.y = paramInt2.intValue();
	//TrxUtil.printStaticUnifiedField(StaticIntPoint.class, "x");
	//TrxUtil.printStaticUnifiedField(StaticIntPoint.class, "y");
    }
    
    public Integer getX()
    {
	// The following code will be instrumented by TrfClassUnifyFields
	// replacing it by something like:
	// return StaticIntPoint.STATIC_PART$.x; 	
      return StaticIntPoint.x;
    }

    public Integer getY()
    {
      return StaticIntPoint.y;
    }

    public void setX(Number paramNumber)
    {
	StaticIntPoint.x = paramNumber.intValue(); // implicit boxing
    }

    public void setY(Number paramNumber)
    {
	StaticIntPoint.y = paramNumber.intValue(); // implicit boxing
    }
}