package org.deuce.transform.jvstm;

import java.util.Iterator;

import org.deuce.objectweb.asm.Opcodes;
import org.deuce.transaction.Context;
import org.deuce.transform.Exclude;

@Exclude
public class DescUtil implements Opcodes{
    
    /*===========================================================================*
     *~~~~~~~~~~~~~~~~~~~~~~~~~~    STATIC METHODS    ~~~~~~~~~~~~~~~~~~~~~~~~~~~*
     *===========================================================================*/
    
    public static int ctorArgumentsSize(String methodDesc, int access){
	int size = 0;
	String lastArgDesc = null;
	for (String  d: DescUtil.argsDescIterator(methodDesc)) {
	    lastArgDesc = d;
	    size += d.equals("J") || d.equals("D")? 2 : 1;
	}
	if(size == 0 || !lastArgDesc.equals(Context.CONTEXT_DESC))
	    return -1;
	else{
	    if((access & ACC_STATIC) == 0) // Added by FMC@14-08-2012 according to a BUG detected in ClassEnhancerPrivCtx
		size++;// include the 'this' argument
	    return size; 
	}
    }
    
    public static Iterable<String> argsDescIterator(final String methodDesc){
	return new Iterable<String>() {public Iterator<String> iterator() {
	    return new MethodArgsDescIterator(methodDesc);
	}};
    }

    /*===========================================================================*
     *~~~~~~~~~~~~~~~~~~~~~~~~~~    INNER CLASSES     ~~~~~~~~~~~~~~~~~~~~~~~~~~~*
     *===========================================================================*/
    
    static class MethodArgsDescIterator implements Iterator<String>{
	final String methodDesc;
	int c;
	StringBuffer current;
	public MethodArgsDescIterator(String methodDesc) {
	    this.methodDesc = methodDesc;
	    c = 1;
	    current = step();
	}
	private StringBuffer step(){
	    StringBuffer current = new StringBuffer();
	    while (true) {
		char car = methodDesc.charAt(c++);
		current.append(car);
		if (car == ')') {
		    return null;
		} else if (car == 'L') {
		    do{
			car = methodDesc.charAt(c++);
			current.append(car);
		    }while(car != ';');
		    return current;
		} else if (car == '[') {
		    while ((car = methodDesc.charAt(c)) == '[') {
			current.append(car);
			++c;
		    }
		} else {
		    return current;
		}
	    }
	}
	@Override
	public String next() {
	    if(current == null) throw new IllegalStateException();
	    String actual = current.toString();
	    current = step();
	    return actual;
	}
	@Override
	public boolean hasNext() {
	    return current != null;
	}

	@Override
	public void remove() {throw new UnsupportedOperationException();}

    };
}
