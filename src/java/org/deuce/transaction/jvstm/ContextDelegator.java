package org.deuce.transaction.jvstm;

import java.util.logging.Logger;

import jvstm.AomBarriers;
import jvstm.ReadWriteTransaction;
import jvstm.Transaction;
import jvstm.TransactionSignaller;
import jvstm.UtilUnsafe;
import jvstm.VBoxAom;

import org.deuce.reflection.UnsafeHolder;
import org.deuce.transaction.Context;
import org.deuce.transaction.TransactionException;
import org.deuce.transform.Exclude;
import org.deuce.transform.jvstm.vboxes.VBoxByteArray;
import org.deuce.transform.jvstm.vboxes.VBoxCharArray;
import org.deuce.transform.jvstm.vboxes.VBoxDoubleArray;
import org.deuce.transform.jvstm.vboxes.VBoxFloatArray;
import org.deuce.transform.jvstm.vboxes.VBoxIntArray;
import org.deuce.transform.jvstm.vboxes.VBoxLongArray;
import org.deuce.transform.jvstm.vboxes.VBoxObjectArray;
import org.deuce.transform.jvstm.vboxes.VBoxShortArray;

/**
 * This delegator should replace the standard org.deuce.transaction.ContextDelegator.
 * The JVSTM barriers will be implemented in this class, instead of the Context
 * implementation because we must distinguish between Object and Arrays access.
 * So the jvstm.Context just implements the handlers for init, commit and rollback
 * methods. The rest of its barriers should throw UnsupoprtedOperationException.
 *
 * @author Fernando Miguel Carvalho
 */
@Exclude
public class ContextDelegator extends org.deuce.transaction.ContextDelegator{

	final static private Logger logger = Logger.getLogger("org.deuce.transaction");
	final private static boolean CAPMEM = Boolean.getBoolean("jvstm.capmem");
	final private static TransactionException READ_ONLY_FAILURE_EXCEPTION =
			new TransactionException("Fail on write (read-only hint was set).");
	final private static CommitException COMMIT_EXCEPTION = new CommitException();
	final private static EarlyAbortException EARLYABORT_EXCEPTION = new EarlyAbortException();


	static{
		/*
		 * Initialize specific JVSTM properties.
		 */
		System.setProperty("org.deuce.transaction.contextClass", org.deuce.transaction.jvstm.Context.class.getName());
		System.setProperty("org.deuce.transform.post",
				org.deuce.transform.jvstm.EnhanceTransactional.class.getName()
				+ "," + org.deuce.transform.jvstm.EnhanceVBoxArrays.class.getName()
				);
		System.setProperty("org.deuce.transform.pre", org.deuce.transform.jvstm.EnhanceStaticFields.class.getName());
		logger.info("********** CapMem analysis = " + CAPMEM + " (disable/enable it in property jvstm.capmem)");
		TransactionSignaller.setSignaller(new TransactionSignaller() {

			@Override
			public void signalEarlyAbort() {
				throw EARLYABORT_EXCEPTION;
			}

			@Override
			public void signalCommitFail(Transaction tx) {
				throw new CommitException(tx);
			}

			@Override
			public void signalCommitFail() {
				throw COMMIT_EXCEPTION;
			}
		});
	}

	private static class CommitException extends TransactionException {
		private static final long serialVersionUID = 1L;
		private final Transaction tx;

		protected CommitException() {
			super();
			this.tx = null;
		}

		protected CommitException(Transaction tx) {
			this.tx = tx;
		}

	}


	private static class EarlyAbortException extends CommitException {
		private static final long serialVersionUID = 1L;
		protected EarlyAbortException() { super(); }
	}

	static public void beforeReadAccess( Object obj, long field, Context context) {
		//useless for the JVSTM
	}

	/**
	 * Performs the captured analysis for the accessed object obj.
	 * If obj was allocated by the current transaction then its ownershiprecord
	 * must be equal to the orecForNewObjects of the current transaction.
	 */
	static private <T> T performCaptureAnalysisForRoTrx(T obj, Context context){
		org.deuce.transaction.jvstm.Context ctx = (org.deuce.transaction.jvstm.Context) context;
		VBoxAom<T> vbox =(VBoxAom<T>) obj;
		if(CAPMEM && (vbox.getOrec() == ctx.currentTrx.orecForNewObjects))
			return obj;
		else
			return ctx.currentTrx.getBoxValue(vbox);

	}

	static public Object onReadAccess( Object obj, Object value, long field, Context context) {
		Object res = performCaptureAnalysisForRoTrx(obj, context);
		return UnsafeHolder.getUnsafe().getObject(res, field);
	}
	static public boolean onReadAccess( Object obj, boolean value, long field, Context context) {
		Object res = performCaptureAnalysisForRoTrx(obj, context);
		return UnsafeHolder.getUnsafe().getBoolean(res, field);
	}
	static public byte onReadAccess( Object obj, byte value, long field, Context context) {
		Object res = performCaptureAnalysisForRoTrx(obj, context);
		return UnsafeHolder.getUnsafe().getByte(res, field);
	}
	static public char onReadAccess( Object obj, char value, long field, Context context) {
		Object res = performCaptureAnalysisForRoTrx(obj, context);
		return UnsafeHolder.getUnsafe().getChar(res, field);
	}
	static public short onReadAccess( Object obj, short value, long field, Context context) {
		Object res = performCaptureAnalysisForRoTrx(obj, context);
		return UnsafeHolder.getUnsafe().getShort(res, field);
	}
	static public int onReadAccess( Object obj, int value, long field, Context context) {
		Object res = performCaptureAnalysisForRoTrx(obj, context);
		return UnsafeHolder.getUnsafe().getInt(res, field);
	}
	static public long onReadAccess( Object obj, long value, long field, Context context) {
		Object res = performCaptureAnalysisForRoTrx(obj, context);
		return UnsafeHolder.getUnsafe().getLong(res, field);
	}
	static public float onReadAccess( Object obj, float value, long field, Context context) {
		Object res = performCaptureAnalysisForRoTrx(obj, context);
		return UnsafeHolder.getUnsafe().getFloat(res, field);
	}
	static public double onReadAccess( Object obj, double value, long field, Context context) {
		Object res = performCaptureAnalysisForRoTrx(obj, context);
		return UnsafeHolder.getUnsafe().getDouble(res, field);
	}

	static public void onWriteAccess( Object obj, Object value, long field, Context context) {
		org.deuce.transaction.jvstm.Context ctx = (org.deuce.transaction.jvstm.Context) context;
		VBoxAom vbox =(VBoxAom) obj;
		if(CAPMEM && (vbox.getOrec() == ctx.currentTrx.orecForNewObjects)) // performs the captured analysis
		UtilUnsafe.UNSAFE.putObject(obj, field, value);
		else{
			if(!(ctx.currentTrx instanceof ReadWriteTransaction))
				throw READ_ONLY_FAILURE_EXCEPTION;
			AomBarriers.put((ReadWriteTransaction) ctx.currentTrx, vbox, value, field);
		}
	}
	static public void onWriteAccess( Object obj, boolean value, long field, Context context) {
		org.deuce.transaction.jvstm.Context ctx = (org.deuce.transaction.jvstm.Context) context;
		VBoxAom vbox =(VBoxAom) obj;
		if(CAPMEM && (vbox.getOrec() == ctx.currentTrx.orecForNewObjects)) // performs the captured analysis
			UtilUnsafe.UNSAFE.putBoolean(obj, field, value);
		else{
			if(!(ctx.currentTrx instanceof ReadWriteTransaction))
				throw READ_ONLY_FAILURE_EXCEPTION;
			AomBarriers.put((ReadWriteTransaction) ctx.currentTrx, vbox, value, field);
		}
	}
	static public void onWriteAccess( Object obj, byte value, long field, Context context) {
		org.deuce.transaction.jvstm.Context ctx = (org.deuce.transaction.jvstm.Context) context;
		VBoxAom vbox =(VBoxAom) obj;
		if(CAPMEM && (vbox.getOrec() == ctx.currentTrx.orecForNewObjects)) // performs the captured analysis
			UtilUnsafe.UNSAFE.putByte(obj, field, value);
		else{
			if(!(ctx.currentTrx instanceof ReadWriteTransaction))
				throw READ_ONLY_FAILURE_EXCEPTION;
			AomBarriers.put((ReadWriteTransaction) ctx.currentTrx, vbox, value, field);
		}
	}
	static public void onWriteAccess( Object obj, char value, long field, Context context) {
		org.deuce.transaction.jvstm.Context ctx = (org.deuce.transaction.jvstm.Context) context;
		VBoxAom vbox =(VBoxAom) obj;
		if(CAPMEM && (vbox.getOrec() == ctx.currentTrx.orecForNewObjects)) // performs the captured analysis
			UtilUnsafe.UNSAFE.putChar(obj, field, value);
		else{
			if(!(ctx.currentTrx instanceof ReadWriteTransaction))
				throw READ_ONLY_FAILURE_EXCEPTION;
			AomBarriers.put((ReadWriteTransaction) ctx.currentTrx, vbox, value, field);
		}
	}
	static public void onWriteAccess( Object obj, short value, long field, Context context) {
		org.deuce.transaction.jvstm.Context ctx = (org.deuce.transaction.jvstm.Context) context;
		VBoxAom vbox =(VBoxAom) obj;
		if(CAPMEM && (vbox.getOrec() == ctx.currentTrx.orecForNewObjects)) // performs the captured analysis
			UtilUnsafe.UNSAFE.putShort(obj, field, value);
		else{
			if(!(ctx.currentTrx instanceof ReadWriteTransaction))
				throw READ_ONLY_FAILURE_EXCEPTION;
			AomBarriers.put((ReadWriteTransaction) ctx.currentTrx, vbox, value, field);
		}
	}
	static public void onWriteAccess( Object obj, int value, long field, Context context) {
		org.deuce.transaction.jvstm.Context ctx = (org.deuce.transaction.jvstm.Context) context;
		VBoxAom vbox =(VBoxAom) obj;
		if(CAPMEM && (vbox.getOrec() == ctx.currentTrx.orecForNewObjects)) // performs the captured analysis
			UtilUnsafe.UNSAFE.putInt(obj, field, value);
		else{
			if(!(ctx.currentTrx instanceof ReadWriteTransaction))
				throw READ_ONLY_FAILURE_EXCEPTION;
			AomBarriers.put((ReadWriteTransaction) ctx.currentTrx, vbox, value, field);
		}
	}
	static public void onWriteAccess( Object obj, long value, long field, Context context) {
		org.deuce.transaction.jvstm.Context ctx = (org.deuce.transaction.jvstm.Context) context;
		VBoxAom vbox =(VBoxAom) obj;
		if(CAPMEM && (vbox.getOrec() == ctx.currentTrx.orecForNewObjects)) // performs the captured analysis
			UtilUnsafe.UNSAFE.putLong(obj, field, value);
		else{
			if(!(ctx.currentTrx instanceof ReadWriteTransaction))
				throw READ_ONLY_FAILURE_EXCEPTION;
			AomBarriers.put((ReadWriteTransaction) ctx.currentTrx, vbox, value, field);
		}
	}
	static public void onWriteAccess( Object obj, float value, long field, Context context) {
		org.deuce.transaction.jvstm.Context ctx = (org.deuce.transaction.jvstm.Context) context;
		VBoxAom vbox =(VBoxAom) obj;
		if(CAPMEM && (vbox.getOrec() == ctx.currentTrx.orecForNewObjects)) // performs the captured analysis
			UtilUnsafe.UNSAFE.putFloat(obj, field, value);
		else{
			if(!(ctx.currentTrx instanceof ReadWriteTransaction))
				throw READ_ONLY_FAILURE_EXCEPTION;
			AomBarriers.put((ReadWriteTransaction) ctx.currentTrx, vbox, value, field);
		}
	}
	static public void onWriteAccess( Object obj, double value, long field, Context context) {
		org.deuce.transaction.jvstm.Context ctx = (org.deuce.transaction.jvstm.Context) context;
		VBoxAom vbox =(VBoxAom) obj;
		if(CAPMEM && (vbox.getOrec() == ctx.currentTrx.orecForNewObjects)) // performs the captured analysis
			UtilUnsafe.UNSAFE.putDouble(obj, field, value);
		else{
			if(!(ctx.currentTrx instanceof ReadWriteTransaction))
				throw READ_ONLY_FAILURE_EXCEPTION;
			AomBarriers.put((ReadWriteTransaction) ctx.currentTrx, vbox, value, field);
		}
	}

	private static final String STATIC_ACCESS_VIOLATION = "This jvstm delegator should be used with the corresponding instrumentation transform.jvstm.EnhanceFieldsUnification!";

	static public void addStaticWriteAccess( Object value, Object obj, long field, Context context) {
		throw new UnsupportedOperationException(STATIC_ACCESS_VIOLATION);
	}
	static public void addStaticWriteAccess( boolean value, Object obj, long field, Context context) {
		throw new UnsupportedOperationException(STATIC_ACCESS_VIOLATION );
	}
	static public void addStaticWriteAccess( byte value, Object obj, long field, Context context) {
		throw new UnsupportedOperationException(STATIC_ACCESS_VIOLATION );
	}
	static public void addStaticWriteAccess( char value, Object obj, long field, Context context) {
		throw new UnsupportedOperationException(STATIC_ACCESS_VIOLATION );
	}
	static public void addStaticWriteAccess( short value, Object obj, long field, Context context) {
		throw new UnsupportedOperationException(STATIC_ACCESS_VIOLATION );
	}
	static public void addStaticWriteAccess( int value, Object obj, long field, Context context) {
		throw new UnsupportedOperationException(STATIC_ACCESS_VIOLATION );
	}
	static public void addStaticWriteAccess( long value, Object obj, long field, Context context) {
		throw new UnsupportedOperationException(STATIC_ACCESS_VIOLATION );
	}
	static public void addStaticWriteAccess( float value, Object obj, long field, Context context) {
		throw new UnsupportedOperationException(STATIC_ACCESS_VIOLATION );
	}
	static public void addStaticWriteAccess( double value, Object obj, long field, Context context) {
		throw new UnsupportedOperationException(STATIC_ACCESS_VIOLATION );
	}

	static public Object onArrayReadAccess( Object[] arr, int index, Context context) {
		throw new UnsupportedOperationException();
	}
	static public byte onArrayReadAccess( byte[] arr, int index, Context context) {
		throw new UnsupportedOperationException();
	}
	static public char onArrayReadAccess( char[] arr, int index, Context context) {
		throw new UnsupportedOperationException();
	}
	static public short onArrayReadAccess( short[] arr, int index, Context context) {
		throw new UnsupportedOperationException();
	}
	static public int onArrayReadAccess( int[] arr, int index, Context context) {
		throw new UnsupportedOperationException();
	}
	static public long onArrayReadAccess( long[] arr, int index, Context context) {
		throw new UnsupportedOperationException();
	}
	static public float onArrayReadAccess( float[] arr, int index, Context context) {
		throw new UnsupportedOperationException();
	}
	static public double onArrayReadAccess( double[] arr, int index, Context context) {
		throw new UnsupportedOperationException();
	}

	static public <T> void onArrayWriteAccess( T[] arr,  int index, T value, Context context) {
		throw new UnsupportedOperationException();
	}
	static public void onArrayWriteAccess( byte[] arr, int index, byte value, Context context) {
		throw new UnsupportedOperationException();
	}
	static public void onArrayWriteAccess( char[] arr, int index, char value, Context context) {
		throw new UnsupportedOperationException();
	}
	static public void onArrayWriteAccess( short[] arr, int index, short value, Context context) {
		throw new UnsupportedOperationException();
	}
	static public void onArrayWriteAccess( int[] arr, int index, int value, Context context) {
		throw new UnsupportedOperationException();
	}
	static public void onArrayWriteAccess( long[] arr, int index, long value, Context context) {
		throw new UnsupportedOperationException();
	}
	static public void onArrayWriteAccess( float[] arr, int index, float value, Context context) {
		throw new UnsupportedOperationException();
	}
	static public void onArrayWriteAccess( double[] arr, int index, double value, Context context) {
		throw new UnsupportedOperationException();
	}

	static public void onIrrevocableAccess(Context context) {
		context.onIrrevocableAccess();
	}

	/*===========================================================================*
	 *~~~~~~~~~~~~~~~~~~~~~~~   ARRAY barriers for VBox Arrays    ~~~~~~~~~~~~~~~*
	 *===========================================================================*/


	static public Object onArrayReadAccess( VBoxObjectArray arr, int index, Context context) {
		arr = performCaptureAnalysisForRoTrx(arr, context);
		return arr.elements[index];
	}
	static public byte onArrayReadAccess( VBoxByteArray arr, int index, Context context) {
		arr = performCaptureAnalysisForRoTrx(arr, context);
		return arr.elements[index];

	}
	static public char onArrayReadAccess( VBoxCharArray arr, int index, Context context) {
		arr = performCaptureAnalysisForRoTrx(arr, context);
		return arr.elements[index];
	}
	static public short onArrayReadAccess( VBoxShortArray arr, int index, Context context) {
		arr = performCaptureAnalysisForRoTrx(arr, context);
		return arr.elements[index];

	}
	static public int onArrayReadAccess( VBoxIntArray arr, int index, Context context) {
		arr = performCaptureAnalysisForRoTrx(arr, context);
		return arr.elements[index];
	}
	static public long onArrayReadAccess( VBoxLongArray arr, int index, Context context) {
		arr = performCaptureAnalysisForRoTrx(arr, context);
		return arr.elements[index];
	}
	static public float onArrayReadAccess( VBoxFloatArray arr, int index, Context context) {
		arr = performCaptureAnalysisForRoTrx(arr, context);
		return arr.elements[index];
	}
	static public double onArrayReadAccess( VBoxDoubleArray arr, int index, Context context) {
		arr = performCaptureAnalysisForRoTrx(arr, context);
		return arr.elements[index];
	}

	static public <T> void onArrayWriteAccess( VBoxObjectArray arr,  int index, T value, Context context) {
		org.deuce.transaction.jvstm.Context ctx = (org.deuce.transaction.jvstm.Context) context;
		if(CAPMEM && (arr.getOrec() == ctx.currentTrx.orecForNewObjects)) // performs the captured analysis
		arr.elements[index] = value;
		else{
			if(!(ctx.currentTrx instanceof ReadWriteTransaction))
				throw READ_ONLY_FAILURE_EXCEPTION;
			ReadWriteTransaction trx = (ReadWriteTransaction) ctx.currentTrx;
			if(trx != null)
				AomBarriers.getTarget(trx, arr).elements[index] = value;
			else
				throw new UnsupportedOperationException("No support for InevitableTransactions yet!");
		}
	}
	static public void onArrayWriteAccess( VBoxByteArray arr, int index, byte value, Context context) {
		org.deuce.transaction.jvstm.Context ctx = (org.deuce.transaction.jvstm.Context) context;
		if(CAPMEM && (arr.getOrec() == ctx.currentTrx.orecForNewObjects)) // performs the captured analysis
			arr.elements[index] = value;
		else{
			if(!(ctx.currentTrx instanceof ReadWriteTransaction))
				throw READ_ONLY_FAILURE_EXCEPTION;
			ReadWriteTransaction trx = (ReadWriteTransaction) ctx.currentTrx;
			if(trx != null)
				AomBarriers.getTarget(trx, arr).elements[index] = value;
			else
				throw new UnsupportedOperationException("No support for InevitableTransactions yet!");
		}
	}
	static public void onArrayWriteAccess( VBoxCharArray arr, int index, char value, Context context) {
		org.deuce.transaction.jvstm.Context ctx = (org.deuce.transaction.jvstm.Context) context;
		if(CAPMEM && (arr.getOrec() == ctx.currentTrx.orecForNewObjects)) // performs the captured analysis
			arr.elements[index] = value;
		else {
			if(!(ctx.currentTrx instanceof ReadWriteTransaction))
				throw READ_ONLY_FAILURE_EXCEPTION;
			ReadWriteTransaction trx = (ReadWriteTransaction) ctx.currentTrx;
			if(trx != null)
				AomBarriers.getTarget(trx, arr).elements[index] = value;
			else
				throw new UnsupportedOperationException("No support for InevitableTransactions yet!");
		}
	}
	static public void onArrayWriteAccess( VBoxShortArray arr, int index, short value, Context context) {
		org.deuce.transaction.jvstm.Context ctx = (org.deuce.transaction.jvstm.Context) context;
		if(CAPMEM && (arr.getOrec() == ctx.currentTrx.orecForNewObjects)) // performs the captured analysis
			arr.elements[index] = value;
		else{
			if(!(ctx.currentTrx instanceof ReadWriteTransaction))
				throw READ_ONLY_FAILURE_EXCEPTION;
			ReadWriteTransaction trx = (ReadWriteTransaction) ctx.currentTrx;
			if(trx != null)
				AomBarriers.getTarget(trx, arr).elements[index] = value;
			else
				throw new UnsupportedOperationException("No support for InevitableTransactions yet!");
		}
	}
	static public void onArrayWriteAccess( VBoxIntArray arr, int index, int value, Context context) {
		org.deuce.transaction.jvstm.Context ctx = (org.deuce.transaction.jvstm.Context) context;
		if(CAPMEM && (arr.getOrec() == ctx.currentTrx.orecForNewObjects)) // performs the captured analysis
			arr.elements[index] = value;
		else{
			if(!(ctx.currentTrx instanceof ReadWriteTransaction))
				throw READ_ONLY_FAILURE_EXCEPTION;
			ReadWriteTransaction trx = (ReadWriteTransaction) ctx.currentTrx;
			if(trx != null)
				AomBarriers.getTarget(trx, arr).elements[index] = value;
			else
				throw new UnsupportedOperationException("No support for InevitableTransactions yet!");
		}
	}
	static public void onArrayWriteAccess( VBoxLongArray arr, int index, long value, Context context) {
		org.deuce.transaction.jvstm.Context ctx = (org.deuce.transaction.jvstm.Context) context;
		if(CAPMEM && (arr.getOrec() == ctx.currentTrx.orecForNewObjects)) // performs the captured analysis
			arr.elements[index] = value;
		else{
			if(!(ctx.currentTrx instanceof ReadWriteTransaction))
				throw READ_ONLY_FAILURE_EXCEPTION;
			ReadWriteTransaction trx = (ReadWriteTransaction) ctx.currentTrx;
			if(trx != null)
				AomBarriers.getTarget(trx, arr).elements[index] = value;
			else
				throw new UnsupportedOperationException("No support for InevitableTransactions yet!");
		}
	}
	static public void onArrayWriteAccess( VBoxFloatArray arr, int index, float value, Context context) {
		org.deuce.transaction.jvstm.Context ctx = (org.deuce.transaction.jvstm.Context) context;
		if(CAPMEM && (arr.getOrec() == ctx.currentTrx.orecForNewObjects)) // performs the captured analysis
			arr.elements[index] = value;
		else{
			if(!(ctx.currentTrx instanceof ReadWriteTransaction))
				throw READ_ONLY_FAILURE_EXCEPTION;
			ReadWriteTransaction trx = (ReadWriteTransaction) ctx.currentTrx;
			if(trx != null)
				AomBarriers.getTarget(trx, arr).elements[index] = value;
			else
				throw new UnsupportedOperationException("No support for InevitableTransactions yet!");
		}
	}
	static public void onArrayWriteAccess( VBoxDoubleArray arr, int index, double value, Context context) {
		org.deuce.transaction.jvstm.Context ctx = (org.deuce.transaction.jvstm.Context) context;
		if(CAPMEM && (arr.getOrec() == ctx.currentTrx.orecForNewObjects)) // performs the captured analysis
			arr.elements[index] = value;
		else{
			if(!(ctx.currentTrx instanceof ReadWriteTransaction))
				throw READ_ONLY_FAILURE_EXCEPTION;
			ReadWriteTransaction trx = (ReadWriteTransaction) ctx.currentTrx;
			if(trx != null)
				AomBarriers.getTarget(trx, arr).elements[index] = value;
			else
				throw new UnsupportedOperationException("No support for InevitableTransactions yet!");
		}
	}
}
