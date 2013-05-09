package org.deuce.benchmark.stmbench7.impl.deucestm;

import org.deuce.Atomic;
import org.deuce.benchmark.stmbench7.OperationExecutor;
import org.deuce.benchmark.stmbench7.core.Operation;
import org.deuce.benchmark.stmbench7.core.OperationFailedException;
import org.deuce.benchmark.stmbench7.core.RuntimeError;

public class DeuceSTMOperationExecutor implements OperationExecutor {

	private final Operation op;
	private boolean readOnly;

	public DeuceSTMOperationExecutor(Operation op) {
		this.op = op;
		switch(op.getOperationId().getType()) {
		case OPERATION_RO:
		case TRAVERSAL_RO:
		case SHORT_TRAVERSAL_RO:
			readOnly = true;
			break; 
		case OPERATION:
		case SHORT_TRAVERSAL:
		case TRAVERSAL:
		case STRUCTURAL_MODIFICATION:
			readOnly = false;
			break;
		default:
			throw new RuntimeError("Unexpected operation type");
		}
	}

	@Override
	public int execute() throws OperationFailedException {
		if(readOnly){
			return txExecuteRo();
		}
		else{
			return txExecuteRw();
		}
	}

	@Atomic(metainf="txExecuteRo")
	private int txExecuteRo() throws OperationFailedException {
		return op.performOperation();
	}

	@Atomic(metainf="txExecuteRw")
	private int txExecuteRw() throws OperationFailedException {
		return op.performOperation();
	}

	@Override
	public int getLastOperationTimestamp() {
		return 0;
	}

}
