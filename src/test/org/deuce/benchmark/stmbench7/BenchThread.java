package org.deuce.benchmark.stmbench7;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

import org.deuce.benchmark.stmbench7.annotations.NonAtomic;
import org.deuce.benchmark.stmbench7.core.Operation;
import org.deuce.benchmark.stmbench7.core.RuntimeError;
import org.deuce.benchmark.stmbench7.core.OperationFailedException;
import org.deuce.transform.Exclude;

@Exclude
class ThreadStats{
	public ThreadStats() {
		this.operationsTTC = new int[OperationId.values().length][Parameters.MAX_LOW_TTC + 1];
		this.operationsHighTTCLog = new int[OperationId.values().length][Parameters.HIGH_TTC_ENTRIES];
	}

	/**
	 * To support multi-dimensional arrays in the Captured Memory feature
	 * we cannot access them out of a transactional method due to the huge 
	 * overhead of unwrapping the encapsulated arrays.
	 * So, we keep multi-dimensional arrays in private fields and access 
	 * them through transactional indexers.  
	 */
	private int[][] operationsTTC, operationsHighTTCLog;

	public int operationsHighTTCLog(int opNumber, int logTtcIndex){
		return operationsHighTTCLog[opNumber][logTtcIndex]; 
	}

	public int operationsTTC(int opNumber, int ttc){
		return operationsTTC[opNumber][ttc];
	}  

	public void incOperationsTTC(int operationNumber, int ttc){
		operationsTTC[operationNumber][ttc]++;
	}

	public void incOperationsHighTTCLog(int operationNumber, int intLogHighTtc){
		operationsHighTTCLog[operationNumber][intLogHighTtc]++;
	}
}
 
/**
 * A single thread of the STMBench7 benchmark. Executes operations assigned to
 * it one by one, randomly choosing the next operation and respecting the
 * expected ratios of operations' counts.
 */
@NonAtomic
public class BenchThread implements Runnable {

	protected volatile boolean stop = false;
	protected double[] operationCDF;
	protected OperationExecutor[] operations;
	protected final short myThreadNum;

	public int[] successfulOperations, failedOperations;
	
	protected final ThreadStats  stats = new ThreadStats();
	
	public class ReplayLogEntry implements Comparable<ReplayLogEntry> {
		public final short threadNum;
		public final int timestamp, result;
		public final boolean failed;
		public final int opNum;

		public ReplayLogEntry(int timestamp, int result, boolean failed,
				int opNum) {
			this.threadNum = myThreadNum;
			this.timestamp = timestamp;
			this.result = result;
			this.failed = failed;
			this.opNum = opNum;
		}

		public int compareTo(ReplayLogEntry entry) {
			return timestamp - entry.timestamp;
		}
	}

	public ArrayList<ReplayLogEntry> replayLog;

	public BenchThread(Setup setup, double[] operationCDF, short myThreadNum) {
		this.operationCDF = operationCDF;

		int numOfOperations = OperationId.values().length;
		successfulOperations = new int[numOfOperations];
		failedOperations = new int[numOfOperations];
		operations = new OperationExecutor[numOfOperations];
		this.myThreadNum = myThreadNum;

		createOperations(setup);

		if (Parameters.sequentialReplayEnabled)
			replayLog = new ArrayList<ReplayLogEntry>();
	}

	protected BenchThread(Setup setup, double[] operationCDF) {
		this.operationCDF = operationCDF;
		operations = new OperationExecutor[OperationId.values().length];
		createOperations(setup);
		myThreadNum = 0;
	}
	
	private static final int OPERATION_NR;
	static{
	    String opId = System.getProperty("uniqueop");
	    if(opId != null && !opId.equals("")){
                OperationId id = OperationId.valueOf(opId); 
	        System.out.println("********** Unique Operation: " + id );
	        OPERATION_NR = id.ordinal();
	    }else{
	        OPERATION_NR = -1;
	    }
	}

	public void run() {
		int i = 0;
		while (!stop) {
			//if (i++ > 55) continue;
		   
			int operationNumber = OPERATION_NR != -1? OPERATION_NR : getNextOperationNumber();

			OperationType type = OperationId.values()[operationNumber].getType();
			//if( (type != OperationType.SHORT_TRAVERSAL) ) continue;
			//		(type != OperationType.SHORT_TRAVERSAL_RO) &&
			//		(type != OperationType.OPERATION) )
			//	continue;

			//System.out.println(i + " > "
			//		+ OperationId.values()[operationNumber]);

			OperationExecutor currentExecutor = operations[operationNumber];
			int result = 0;
			boolean failed = false;

			try {
				long startTime = System.currentTimeMillis();

				result = currentExecutor.execute();

				long endTime = System.currentTimeMillis();
				//System.out.println("success");

				successfulOperations[operationNumber]++;
				int ttc = (int) (endTime - startTime);
				if (ttc <= Parameters.MAX_LOW_TTC)
				        stats.incOperationsTTC(operationNumber, ttc);
				else {
					double logHighTtc = (Math.log(ttc) - Math
							.log(Parameters.MAX_LOW_TTC + 1))
							/ Math.log(Parameters.HIGH_TTC_LOG_BASE);
					int intLogHighTtc = Math.min((int) logHighTtc,
							Parameters.HIGH_TTC_ENTRIES - 1);
					stats.incOperationsHighTTCLog(operationNumber, intLogHighTtc);
				}
			} catch (OperationFailedException e) {
				//System.out.println("failed");
				failedOperations[operationNumber]++;
				failed = true;
			}

			if (Parameters.sequentialReplayEnabled) {
				ReplayLogEntry newEntry = new ReplayLogEntry(
						currentExecutor.getLastOperationTimestamp(), result, failed,
						operationNumber);
				replayLog.add(newEntry);
				//System.out.println("ts: " + newEntry.timestamp);
			}
		}
		System.err.print("#" + myThreadNum + " ");
		System.err.flush();
		//i = 0;
		//for (ReplayLogEntry entry : replayLog)
		//	System.out.println(i++ + " % " + OperationId.values()[entry.opNum]
		//			+ " -- " + entry.timestamp);
	}

	public void stopThread() {
		stop = true;
	}

	protected void createOperations(Setup setup) {
		for (OperationId operationDescr : OperationId.values()) {
			Class<? extends Operation> operationClass = operationDescr
					.getOperationClass();
			int operationIndex = operationDescr.ordinal();

			try {
				Constructor<? extends Operation> operationConstructor = operationClass
						.getConstructor(Setup.class);
				Operation operation = operationConstructor.newInstance(setup);

				operations[operationIndex] = OperationExecutorFactory.instance
						.createOperationExecutor(operation);
				assert (operation.getOperationId().getOperationClass()
						.equals(operationClass));
			} catch (Exception e) {
				throw new RuntimeError("Error while creating operation "
						+ operationDescr, e);
			}
		}
	}

	protected int getNextOperationNumber() {
		double whichOperation = ThreadRandom.nextDouble();
		int operationNumber = 0;
		while (whichOperation >= operationCDF[operationNumber])
			operationNumber++;
		return operationNumber;
	}
}
