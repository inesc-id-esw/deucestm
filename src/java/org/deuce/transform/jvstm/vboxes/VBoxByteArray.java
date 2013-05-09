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
public class VBoxByteArray extends AbstractVBoxArray<VBoxByteArray>{
    public final byte [] elements;

    public VBoxByteArray(byte [] elements) {
	super();
	this.elements = elements;
    }
    
    public VBoxByteArray(int length, Transaction trx) {
        super(trx);
	this.elements = new byte [length];
    }

    public VBoxByteArray(byte [] elements, Transaction trx) {
        super(trx);
        this.elements = elements;
    }
    
    @Override
    public int arrayLength() {
	return elements.length;
    }
    
    @Override
    public VBoxByteArray replicate() {
        return new VBoxByteArray(elements.clone());
    }
    
    @Override
    public void toCompactLayout(VBoxByteArray from) {
        System.arraycopy(from.elements, 0, this.elements, 0, from.elements.length);
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, int length) {
        System.arraycopy(this.elements, srcPos, ((VBoxByteArray)dest).elements, destPos, length);
    }

}
