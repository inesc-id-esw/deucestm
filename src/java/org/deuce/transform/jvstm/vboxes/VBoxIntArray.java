package org.deuce.transform.jvstm.vboxes;

import jvstm.Transaction;

import org.deuce.transform.Exclude;


/**
 * The instances of this class encapsulates an array 
 * and keeping its captured state.  
 *  
 * @author mcarvalho
 */
@Exclude
public class VBoxIntArray extends AbstractVBoxArray<VBoxIntArray>{
    public final int [] elements;

    public VBoxIntArray(int [] elements) {
	this.elements = elements;
    }
    
    public VBoxIntArray(int length, Transaction trx) {
        super(trx);
	this.elements = new int [length];
    }

    public VBoxIntArray(int [] elements, Transaction trx) {
        super(trx);
        this.elements = elements;
    }
    
    @Override
    public int arrayLength() {
	return elements.length;
    }
    
    @Override
    public VBoxIntArray replicate() {
        return new VBoxIntArray(elements.clone());
    }
    
    @Override
    public void toCompactLayout(VBoxIntArray from) {
        System.arraycopy(from.elements, 0, this.elements, 0, from.elements.length);
    }
    
    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, int length) {
        System.arraycopy(this.elements, srcPos, ((VBoxIntArray)dest).elements, destPos, length);
    }
}
