package org.deuce.transform.jvstm;

import org.deuce.transform.Exclude;

@Exclude
public class JvstmUtils implements JvstmConstants{
    
    /*===========================================================================*
     *~~~~~~~~~~~~~~~~~~~~~~~~~  AUXILIARY FUNCTIONS  ~~~~~~~~~~~~~~~~~~~~~~~~~~~*
     *===========================================================================*/
    
    public static String parametrizeVBoxDesc(String className){
	return "L" + VBOX_INTERNAL + "<L"+ className + ";>;";
    }

}
