package jstamp.ssca2;

import org.deuce.Atomic;

public class GraphSDG {
    int[] startVertex;
    int[] endVertex;
    int[] intWeight;

    /* The idea is to store the index of the string weights (as a negative value)
     * in the int Weight array. A negative value because we need to sort on
     * the intWeights in Kernel 2. Hence the int int
     */
    byte[] strWeight;
    private int numEdgesPlaced;

    public GraphSDG() {

    }

    @Atomic
    public int startVertex(int i) {
        return startVertex[i];
    }

    @Atomic
    public int[] intWeight() {
        return intWeight;
    }

    @Atomic
    public byte[] strWeight() {
        return strWeight;
    }

    @Atomic
    public byte strWeight(int i) {
        return strWeight[i];
    }

    @Atomic
    public int numEdgesPlaced() {
        return numEdgesPlaced;
    }

    @Atomic
    public int endVertex(int t) {
        return endVertex[t];
    }

    @Atomic
    public void set_startVertex(int[] val) {
        startVertex = val;
    }
    
    @Atomic
    public void set_endVertex(int[] val) {
        endVertex = val;
    }

    @Atomic
    public void set_numEdgesPlaced(int numEdgesPlaced2) {
        numEdgesPlaced = numEdgesPlaced2;
    }

    @Atomic
    public void set_intWeight(int[] is) {
        intWeight = is;
    }

    @Atomic
    public void set_intWeight(int i, int val) {
        intWeight[i] = val;
    }

    @Atomic
    public int intWeight(int i) {
        return intWeight(i);
    }

    @Atomic
    public void set_strWeight(byte[] bs) {
        strWeight = bs;
    }
    
    @Atomic
    public void set_strWeight(int i, byte val) {
        strWeight[i] = val;
    }

    @Atomic
    public int[] startVertex() {
        return startVertex;
    }
    
    @Atomic
    public int[] endVertex() {
        return endVertex;
    }

    @Atomic
    public void set_endVertex(int j, int val) {
        endVertex[j] = val;
    }
}
