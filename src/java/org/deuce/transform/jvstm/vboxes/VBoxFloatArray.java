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
public class VBoxFloatArray extends AbstractVBoxArray<VBoxFloatArray>{
    public final float [] elements;

    public VBoxFloatArray(float [] elements) {
	super();
	this.elements = elements;
    }
    
    public VBoxFloatArray(int length, Transaction trx) {
        super(trx);
	this.elements = new float [length];
    }
    
    public VBoxFloatArray(float [] elements, Transaction trx) {
        super(trx);
        this.elements = elements;
    }
    
    @Override
    public int arrayLength() {
	return elements.length;
    }

    @Override
    public VBoxFloatArray replicate() {
        return new VBoxFloatArray(elements.clone());
    }
    
    @Override
    public void toCompactLayout(VBoxFloatArray from) {
        System.arraycopy(from.elements, 0, this.elements, 0, from.elements.length);
    }
    
    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, int length) {
        System.arraycopy(this.elements, srcPos, ((VBoxFloatArray)dest).elements, destPos, length);
    }
}
