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
public class VBoxCharArray extends AbstractVBoxArray<VBoxCharArray>{
    public final char [] elements;

    public VBoxCharArray(char [] elements) {
	super();
	this.elements = elements;
    }
    
    public VBoxCharArray(int length, Transaction trx) {
        super(trx);
	this.elements = new char [length];
    }
    
    public VBoxCharArray(char [] elements, Transaction trx) {
        super(trx);
        this.elements = elements;
    }
    

    @Override
    public int arrayLength() {
	return elements.length;
    }

    @Override
    public VBoxCharArray replicate() {
        return new VBoxCharArray(elements.clone());
    }
    
    @Override
    public void toCompactLayout(VBoxCharArray from) {
        System.arraycopy(from.elements, 0, this.elements, 0, from.elements.length);
    }
    
    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, int length) {
        System.arraycopy(this.elements, srcPos, ((VBoxCharArray)dest).elements, destPos, length);
    }

}
