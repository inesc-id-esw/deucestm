package org.deuce.benchmark.jwormbench;

import org.deuce.Atomic;

import jwormbench.core.INode;
import jwormbench.core.IWorm;
import jwormbench.factories.INodeFactory;

public class DeuceNodeFactory implements INodeFactory{

    @Override
    public INode make(int value) {
        return new DeuceNode(value);
    }

}

class DeuceNode implements INode{
    IWorm worm;
    int value;
    
    public DeuceNode(int value) {
        this.value = value;
    }
    public IWorm getWorm() {
        return worm;
    }
    public void setWorm(IWorm worm) {
        this.worm = worm;
    }
    @Atomic
    public int getValue() {
        return value;
    }
    @Atomic
    public void setValue(int value) {
        this.value = value;
    }
    
    
}