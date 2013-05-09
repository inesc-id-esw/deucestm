package org.deuce.transform.jvstm.vboxes;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import jvstm.Transaction;

import org.deuce.objectweb.asm.Type;
import org.deuce.transform.Exclude;


/**
 * @author mcarvalho
 */
@Exclude
public class VBoxObjectArray extends AbstractVBoxArray<VBoxObjectArray>{
    public Object [] elements;
    public int nrOfDimensions;
    public int[] dimLengths;
    public Class componentClass;
    
    @Override
    public int arrayLength() {
	return elements.length;
    }

    /**
     * This constructor may be invoked from regular methods (non-transactional) when they
     * need to update array fields that are encapsulated in VBoxArrays.
     * This constructor could be also invoked for multi-arrays purpose.
     */

    public VBoxObjectArray(Object [] elements) {
	if(elements != null && elements.getClass().getName().lastIndexOf('[') > 0)
	    processMultiArray(elements, null);
	else 
	    this.elements = elements;
    }

    /**
     * Invoked from transactional methods.
     * @param type The component type of the array being instantiated. 
     */
    public VBoxObjectArray(int length, String type, Transaction trx) {
        super(trx);
        if(type.charAt(0) == '['){
            this.elements = new Object [length];
        }else{
            try {
                Class<?> componentType = Class.forName(Type.getObjectType(type).getClassName());
                this.elements = (Object[]) Array.newInstance(componentType, length);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * This constructor is invoked for multi-arrays purpose.
     */

    public VBoxObjectArray(Object [] elements, Transaction trx) {
        super(trx);
        processMultiArray(elements, trx);
    }

    
    public void processMultiArray(Object [] elements, Transaction trx) {
	Class firstDimElemClass = elements.getClass().getComponentType(); // = int[] if elements is int[][]
	Class scondDimElemClass  = firstDimElemClass.getComponentType(); // = int if elements is int[][]

	if(scondDimElemClass == null){
	    this.elements = elements;
	    return;
	}
	
	nrOfDimensions = 0;
	componentClass = elements.getClass();
	while ( componentClass.isArray() ) {
	    nrOfDimensions++;
	    componentClass = componentClass.getComponentType();
	}
	dimLengths = new int[nrOfDimensions];
	Object arr = elements;
	for (int i = 0; i < dimLengths.length; i++) {
	    dimLengths[i] = arr == null? 0 : Array.getLength(arr);
	    arr = ((arr == null) || (dimLengths[i] == 0)) ? null : Array.get(arr, 0);
	}
	
	Object [] aux = new Object[elements.length];
	for (int i = 0; i < aux.length; i++) {
	    if(scondDimElemClass.isArray()){ 
		// if elements is e.g. int[][][]
	        if(trx == null)
	            aux[i] = new VBoxObjectArray((Object[]) elements[i]);
	        else
	            aux[i] = new VBoxObjectArray((Object[]) elements[i], trx);
	    }else{
		// if elements is e.g. int[][]
		aux[i] = newWrapper(firstDimElemClass, scondDimElemClass, elements[i], trx); // then create a VBoxIntArray 
	    }
	}
	this.elements = aux;
    }
    
    private static Object newWrapper(Class firstDimElemClass, Class scondDimElemClass, Object innerArray, Transaction trx) {
	if(!scondDimElemClass.isPrimitive()){
	    if(trx == null)
		return new VBoxObjectArray((Object[]) innerArray);
	    else
	        return new VBoxObjectArray((Object[]) innerArray, trx);
	}else{
	    // e.g. firstDimElemClass = int[] and scondDimElemClass = int
	    String primName = scondDimElemClass.getName(); // = int
	    primName = Character.toUpperCase(primName.charAt(0)) + primName.substring(1); // = Int
	    try {
		Class wrapper = Class.forName(VBoxObjectArray.class.getName().replace("Object", primName)); // = VBoxIntArray
		if(trx == null){
    		    Constructor ctor = wrapper.getConstructor(firstDimElemClass); // = ctor(int[])
    		    return ctor.newInstance(innerArray);
		} else {
		    Constructor ctor = wrapper.getConstructor(firstDimElemClass, Transaction.class); // = ctor(int[])
                    return ctor.newInstance(innerArray, trx);
		}
	    } catch (ClassNotFoundException e) {
		throw new RuntimeException(e);
	    } catch (NoSuchMethodException e) {
		throw new RuntimeException(e);
	    } catch (SecurityException e) {
		throw new RuntimeException(e);
	    } catch (InstantiationException e) {
		throw new RuntimeException(e);
	    } catch (IllegalAccessException e) {
		throw new RuntimeException(e);
	    } catch (IllegalArgumentException e) {
		throw new RuntimeException(e);
	    } catch (InvocationTargetException e) {
		throw new RuntimeException(e);
	    } 
	}
    }
    /**
     * This method is for when regular methods (non-transactional) access arrays wrapped in VBox.
     * Yet this operations has a huge overhead and in this case we prefer to throw an exception 
     * alerting the user programmer to access the owner class from a transactional method, instead 
     * of accessing it from a non-transactional method, which in turn requires the encapsulated array 
     * to be unwrapped. 
     */
    public Object unwrapp() throws Exception{
	// this method will fail if it unwrapps unidimensional
	Object [] aux = (Object []) Array.newInstance(componentClass, dimLengths);
	unwrapp(aux);
	return aux;
    }

    private void unwrapp(Object[] newElems)  throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
	if(elements.length == 0) 
	    return;
	
	if(!(elements[0] instanceof AbstractVBoxArray)){
	    for (int i = 0; i < elements.length; i++) {
		newElems[i] = elements[i];  
	    }
	}
	else if(elements[0] instanceof VBoxObjectArray){
	    for (int i = 0; i < elements.length; i++) {
		((VBoxObjectArray)elements[i]).unwrapp((Object[]) newElems[i]);
	    }
	}else{
	    for (int i = 0; i < elements.length; i++) {
	        Field elems = elements[i].getClass().getDeclaredField("elements");
                newElems[i] = elems.get(elements[i]);
	    }
	}

    }
    
    @Override
    public VBoxObjectArray replicate() {
        return new VBoxObjectArray(elements.clone());
    }
    
    @Override
    public void toCompactLayout(VBoxObjectArray from) {
        System.arraycopy(from.elements, 0, this.elements, 0, from.elements.length);
    }
    
    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, int length) {
        System.arraycopy(this.elements, srcPos, ((VBoxObjectArray)dest).elements, destPos, length);
    }
}
