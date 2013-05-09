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
public class VBoxDoubleArray extends AbstractVBoxArray<VBoxDoubleArray>{
    public final double [] elements;

    public VBoxDoubleArray(double [] elements) {
	super();
	this.elements = elements;
    }
    
    public VBoxDoubleArray(int length, Transaction trx) {
        super(trx);
	this.elements = new double [length];
    }

    public VBoxDoubleArray(double [] elements, Transaction trx) {
        super(trx);
        this.elements = elements;
    }

    @Override
    public int arrayLength() {
	return elements.length;
    }

    @Override
    public VBoxDoubleArray replicate() {
        return new VBoxDoubleArray(elements.clone());
    }
    
    @Override
    public void toCompactLayout(VBoxDoubleArray from) {
        System.arraycopy(from.elements, 0, this.elements, 0, from.elements.length);
    }
    
    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, int length) {
        System.arraycopy(this.elements, srcPos, ((VBoxDoubleArray)dest).elements, destPos, length);
    }
}
