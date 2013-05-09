package org.deuce.transaction.jvstm;

import static jvstm.UtilUnsafe.UNSAFE;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

import org.deuce.transform.Exclude;

@Exclude
public class Util {

    /*===========================================================================*
     *~~~~~~~~~~~~~~~~~~~~~~~        PRIVATE CONSTANTS      ~~~~~~~~~~~~~~~~~~~~~*
     *===========================================================================*/
    
    private static final int NR_BITS = Integer.valueOf(System.getProperty("sun.arch.data.model"));
    private static final int BYTE = 8;
    private static final int WORD = NR_BITS/BYTE;
    private static final int MIN_SIZE = 16; 
    
    /*===========================================================================*
     *~~~~~~~~~~~~~~~~~~~~~~~        AUXILIAR FUNCTION      ~~~~~~~~~~~~~~~~~~~~~*
     *===========================================================================*/

    public static long sizeOf(Class src){
        if(src == Object.class) return MIN_SIZE;         
        List<Field> instanceFields = new LinkedList<Field>();
        do{
            if(src == Object.class) return MIN_SIZE;
            for (Field f : src.getDeclaredFields()) {
                if((f.getModifiers() & Modifier.STATIC) == 0){
                    instanceFields.add(f);
                }
            }
            src = src.getSuperclass();
        }while(instanceFields.isEmpty());
        
        long maxOffset = 0;
        for (Field f : instanceFields) {
            long offset = UNSAFE.objectFieldOffset(f);
            if(offset > maxOffset) maxOffset = offset; 
        }
        return  ((maxOffset/WORD) + 1)*WORD; 
    }
}
