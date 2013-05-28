package org.deuce.transaction.jvstm;

import java.util.logging.Logger;

import jvstm.TopLevelReadTransaction;
import jvstm.Transaction;

import org.deuce.transaction.TransactionException;
import org.deuce.transaction.util.BooleanArrayList;
import org.deuce.transform.Exclude;

/**
 * Integration with the JVSTM-AOM.
 * => object level conflict detection;
 * => the transactional classes inherit from VBoxAom to include an extra field (vbody), 
 *    which stores the versions history;
 *  
 * @author Fernando Miguel Carvalho
 */
@Exclude
public class Context implements org.deuce.transaction.Context{

	final private static boolean RO_HINT;
	final static private Logger logger = Logger.getLogger("org.deuce.transaction");

	static{
		RO_HINT = Boolean.getBoolean("org.deuce.transaction.jvstm.rohint");
		logger.info("********** JVSTM RO_HINT = " + RO_HINT + " (disable/enable it in property org.deuce.transaction.jvstm.rohint)");
		/*
		 * Print number of aborted transactions
		 */ 
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				// logger.info("Nr of aborted trxs: " + Transaction.nrOfAborts);
				// logger.info("Nr of reversions: " + ActiveTransactionsRecord.nrOfReversions + NEW_LINE);
				logger.info("Nr of commits: " + nrOfCommits);
				logger.info("Nr of aborts: " + nrOfAborts);
				// logger.info("Nr of aborts by Rare objects: " + TopLevelTransaction.abortedByRareObject);
				// VBoxAom.printRevLog();
			}
		});
	}
	/* =============================================================== *
	 * ------------------- CONTEXT IMPLEMENTATION -------------------- *
	 * =============================================================== */    
	public Transaction currentTrx;

	// Keep per-thread read-only hints (uses more memory but faster)
	final private BooleanArrayList readWriteMarkers = new BooleanArrayList();
	private boolean readOnlyHint = true;
	private int atomicBlockId;
	private static int nrOfCommits = 0;
	private static int nrOfAborts = 0;

	public Transaction getCurrentTrx(){
		return currentTrx;
	}

	@Override
	public void init(int blockId, String metainf) {
		if(RO_HINT){
			this.atomicBlockId = blockId;
			readOnlyHint = readWriteMarkers.get(blockId) == false;
			currentTrx = Transaction.begin(readOnlyHint);
		}
		else{
			boolean ro = (metainf != null) && metainf.equals("RO");
			currentTrx = Transaction.begin(ro);
			// logger.info("META: " + metainf + " ... " + currentTrx.getClass());
		}
/*
 * Pseudo code describing Deuce transactional control flow for an
 * atomic method.  
 */
/*
		for  (int  i  = 64; i  > 0; -- i) {
			context.init ();
			try {
				result  = contains(v, context);
			} catch(TransactionException ex) {
				// Must rollback
				commit = false;
			} catch(Throwable ex) {
				throwable = ex;
			}
			if  (commit) {
				if  (context.commit()) {
					if  (throwable == null)
						return result;
					// Rethrow application exception
					throw throwable;
				}
			} else  {
				context.rollback ();
				commit = true;
			}
		} // Retry loop
		throw new TransactionException();
*/
	}

	@Override
	public boolean commit() {
		try {
			currentTrx.commitTx(true); // Commit and finish also.
			nrOfCommits++;
		} catch (TransactionException e) {
			// According to the algorithm for atomic methods above
			// in this case we must explicitly invoke the rollback.
			rollback();  
			return false;
		} finally{
			/*
			 * In the case of a nested transaction, when it finishes we must update the 
			 * currentTrx with its parent that has been already up to date in the
			 * transaction's context.
			 */
			currentTrx = Transaction.current(); 
		}
		return true;
	}

	@Override
	public void rollback() {
		nrOfAborts++;
		if(RO_HINT && readOnlyHint) {
			// Change hint to read-write
			readWriteMarkers.insert(atomicBlockId, true);

		}
		if (!RO_HINT && currentTrx instanceof TopLevelReadTransaction){
			throw new Error("Read-Only Transactions should never fail!");
		}
		currentTrx.abortTx();
		/*
		 * In the case of a nested transaction, when it finishes we must update the 
		 * currentTrx with its parent that has been already up to date in the
		 * transaction's context.
		 */
		currentTrx = Transaction.current(); 
	}

	@Override
	public void beforeReadAccess(Object obj, long field) {
		//useless for the JVSTM
	}

	private static final String BARRIER_VIOLATION = "This jvstm.Context should be used with the corresponding delegator jvstm.ContextDelegator! The last invoker class should be instrumented with the correct delegator.";

	@Override
	public Object onReadAccess(Object obj, Object value, long field) {
		throw new UnsupportedOperationException(BARRIER_VIOLATION );
	}

	@Override
	public boolean onReadAccess(Object obj, boolean value, long field) {
		throw new UnsupportedOperationException(BARRIER_VIOLATION );    }

	@Override
	public byte onReadAccess(Object obj, byte value, long field) {
		throw new UnsupportedOperationException(BARRIER_VIOLATION );
	}

	@Override
	public char onReadAccess(Object obj, char value, long field) {
		throw new UnsupportedOperationException(BARRIER_VIOLATION );
	}

	@Override
	public short onReadAccess(Object obj, short value, long field) {
		throw new UnsupportedOperationException(BARRIER_VIOLATION );
	}

	@Override
	public int onReadAccess(Object obj, int value, long field) {
		throw new UnsupportedOperationException(BARRIER_VIOLATION );
	}

	@Override
	public long onReadAccess(Object obj, long value, long field) {
		throw new UnsupportedOperationException(BARRIER_VIOLATION );
	}

	@Override
	public float onReadAccess(Object obj, float value, long field) {
		throw new UnsupportedOperationException(BARRIER_VIOLATION );
	}

	@Override
	public double onReadAccess(Object obj, double value, long field) {
		throw new UnsupportedOperationException(BARRIER_VIOLATION );
	}

	@Override
	public void onWriteAccess(Object obj, Object value, long field) {
		throw new UnsupportedOperationException(BARRIER_VIOLATION );
	}

	@Override
	public void onWriteAccess(Object obj, boolean value, long field) {
		throw new UnsupportedOperationException(BARRIER_VIOLATION );
	}

	@Override
	public void onWriteAccess(Object obj, byte value, long field) {
		throw new UnsupportedOperationException(BARRIER_VIOLATION );
	}

	@Override
	public void onWriteAccess(Object obj, char value, long field) {
		throw new UnsupportedOperationException(BARRIER_VIOLATION );
	}

	@Override
	public void onWriteAccess(Object obj, short value, long field) {
		throw new UnsupportedOperationException(BARRIER_VIOLATION );
	}

	@Override
	public void onWriteAccess(Object obj, int value, long field) {
		throw new UnsupportedOperationException(BARRIER_VIOLATION );
	}

	@Override
	public void onWriteAccess(Object obj, long value, long field) {
		throw new UnsupportedOperationException(BARRIER_VIOLATION );
	}

	@Override
	public void onWriteAccess(Object obj, float value, long field) {
		throw new UnsupportedOperationException(BARRIER_VIOLATION );
	}

	@Override
	public void onWriteAccess(Object obj, double value, long field) {
		throw new UnsupportedOperationException(BARRIER_VIOLATION );

	}

	@Override
	public void onIrrevocableAccess() {
		// TODO Auto-generated method stub
	}
}
