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
public class VBoxShortArray extends AbstractVBoxArray<VBoxShortArray>{
    public final short [] elements;

    public VBoxShortArray(short [] elements) {
	this.elements = elements;
    }
    
    public VBoxShortArray(int length, Transaction trx) {
        super(trx);
	this.elements = new short [length];
    }

    public VBoxShortArray(short [] elements, Transaction trx) {
        super(trx);
        this.elements = elements;
    }
    
    @Override
    public int arrayLength() {
	return elements.length;
    }
 
    @Override
    public VBoxShortArray replicate() {
        return new VBoxShortArray(elements.clone());
    }
    
    @Override
    public void toCompactLayout(VBoxShortArray from) {
        System.arraycopy(from.elements, 0, this.elements, 0, from.elements.length);
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, int length) {
        System.arraycopy(this.elements, srcPos, ((VBoxShortArray)dest).elements, destPos, length);
    }
}
