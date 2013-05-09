package org.deuce.transform.jvstm;

import jvstm.OwnershipRecord;
import jvstm.UtilUnsafe;
import jvstm.VBoxAom;
import jvstm.VBoxBody;

import org.deuce.objectweb.asm.Type;
import org.deuce.transform.Exclude;

@Exclude
public interface JvstmConstants{
    
    /*===========================================================================*
     *~~~~~~~~~~~~~~~~~~~~~~~~~~       CONSTANTS      ~~~~~~~~~~~~~~~~~~~~~~~~~~~*
     *===========================================================================*/
    
    public final static String SUFFIX_STATICFIELDS = "__STATICFIELDS__";
    public final static String SUFFIX_FIELDS = "__FIELDS__";
    public final static String SUFFIX_ADDRESS= "__ADDRESS__";
    
    public final static String OBJECT_INTERNAL = Type.getInternalName(Object.class);
    public final static String OBJECT_DESC = Type.getDescriptor(Object.class);
    public final static String VBOX_INTERNAL = Type.getInternalName(VBoxAom.class);
    public final static String VBOX_OFFSETS_INTERNAL  = Type.getInternalName(org.deuce.transaction.jvstm.Util.class);
    public final static long DOUBLELAYOUT_FIELDS_SIZE = 8; 
    
    public final static String CLASS_VBODY = Type.getInternalName(VBoxBody.class);
    public final static String CLASS_OREC = Type.getInternalName(OwnershipRecord.class);
    public final static String CLASS_UNSAFE = Type.getInternalName(UtilUnsafe.class);
    public final static String CLASS_JVSTM_CTX = Type.getInternalName(org.deuce.transaction.jvstm.Context.class);
    public final static String CLASS_JVSTM_TRX_DESC = Type.getDescriptor(jvstm.Transaction.class);
    
    // public final static String TRANSACTIONAL_DESC = Type.getDescriptor(Transactional.class);
    // public final static String NOSYNC_DESC = Type.getDescriptor(NoSync.class);

    public final static String FIELD_NAME_OBJECT_SIZE = "OBJECT_SIZE"; 
    public final static String FIELD_NAME_STATIC_PART = "STATIC_PART$";
    public final static String FIELD_NAME_CURRENT_TRX = "currentTrx";
    
}
