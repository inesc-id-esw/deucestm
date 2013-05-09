package org.deuce.transform.jvstm.vboxes;

import org.deuce.transform.Exclude;

import jvstm.Transaction;
import jvstm.VBox;
import jvstm.VBoxAom;

/**
 * VBox wrapper for array types.
 * Every inherited class must declare an elements field array.   
 */
@Exclude
public abstract class AbstractVBoxArray<E> extends VBoxAom<E>{
    
    public AbstractVBoxArray(){
    }
    
    /**
     * In this case the object will be instantiated in captured memory,
     * corresponding to memory  allocated inside a transaction that 
     * cannot escape (i.e., is captured by) its allocating transaction.
     */
    public AbstractVBoxArray(Transaction owner){
        super(owner);
    }
    
    public abstract int arrayLength();
    public abstract void arraycopy(int srcPos, Object dest, int destPos, int length);
}
