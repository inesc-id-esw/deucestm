package org.deuce.utest.jvstm;

import org.deuce.Atomic;

public class EnumTestTarget {

	public void checkDirection(Direction dir) throws Throwable{
		switch(dir){}
	}

	@Atomic
	public void checkDirectionInAtomicScope(Direction dir) throws Exception{        
		switch(dir){}
	}
}
