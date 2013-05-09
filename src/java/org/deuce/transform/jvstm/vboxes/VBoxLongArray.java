package org.deuce.transform.jvstm.vboxes;

import jvstm.Transaction;

import org.deuce.transaction.Context;
import org.deuce.transform.Exclude;


/**
 * The instances of this class encapsulates an array 
 * and keeping its captured state.  
 *  
 * @author mcarvalho
 */
@Exclude
public class VBoxLongArray extends AbstractVBoxArray<VBoxLongArray>{
    public final long [] elements;

    public VBoxLongArray(long [] elements) {
	this.elements = elements;
    }
    
    public VBoxLongArray(int length, Transaction trx) {
        super(trx);
	this.elements = new long [length];
    }
    
    public VBoxLongArray(long [] elements, Transaction trx) {
        super(trx);
        this.elements = elements;
    }
    
    @Override
    public int arrayLength() {
	return elements.length;
    }

    @Override
    public VBoxLongArray replicate() {
        return new VBoxLongArray(elements.clone());
    }
    
    @Override
    public void toCompactLayout(VBoxLongArray from) {
        System.arraycopy(from.elements, 0, this.elements, 0, from.elements.length);
    }
    
    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, int length) {
        System.arraycopy(this.elements, srcPos, ((VBoxLongArray)dest).elements, destPos, length);
    }
}
