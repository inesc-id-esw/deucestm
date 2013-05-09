package jstamp.ssca2;

import org.deuce.Atomic;

/* Graph data structure*/
public class Graph {
    int numVertices;
    int numEdges;

    int numDirectedEdges;
    int numUndirectedEdges;

    int numIntEdges;
    int numStrEdges;

    int[] outDegree;
    int[] outVertexIndex;
    int[] outVertexList;
    int[] paralEdgeIndex;

    int[] inDegree;
    int[] inVertexIndex;
    int[] inVertexList;

    int[]  intWeight;
    byte[] strWeight;

    public Graph() {

    }

    @Atomic
    public int numVertices() {
        return numVertices;
    }
    @Atomic
    public void set_numDirectedEdges(int outVertexListSize) {
        numDirectedEdges = outVertexListSize;
    }

    @Atomic
    public void set_outVertexList(int[] is) {
        outVertexList = is;
    }

    @Atomic
    public void set_paralEdgeIndex(int[] is) {
        paralEdgeIndex = is;
    }

    @Atomic
    public void set_outVertexList(int i, int endVertex) {
        outVertexList[i] = endVertex;
    }

    @Atomic
    public void set_inDegree(int[] is) {
        inDegree = is;
    }

    @Atomic
    public void set_inVertexIndex(int[] is) {
        inVertexIndex = is;
    }

    @Atomic
    public int[] inVertexIndex() {
        return inVertexIndex;
    }

    @Atomic
    public int[] inDegree() {
        return inDegree;
    }

    @Atomic
    public int inVertexIndex(int i) {
        return inVertexIndex[i];
    }

    @Atomic
    public int inDegree(int i) {
        return inDegree[i];
    }

    @Atomic
    public void set_numUndirectedEdges(int i) {
        numUndirectedEdges = i;
    }

    @Atomic
    public void set_inVertexList(int[] is) {
        inVertexList = is;
    }

    @Atomic
    public int numUndirectedEdges() {
        return numUndirectedEdges;
    }

    @Atomic
    public int[] outVertexIndex() {
        return outVertexIndex;
    }

    @Atomic
    public int[] outDegree() {
        return outDegree;
    }

    @Atomic
    public int intWeight(int i) {
        return intWeight[i];
    }

    @Atomic
    public int numStrEdges() {
        return numStrEdges;
    }

    @Atomic
    public int outVertexIndex(int t) {
        return outVertexIndex[t];
    }

    @Atomic
    public int outVertexList(int i) {
        return outVertexList[i];
    }

    @Atomic
    public int paralEdgeIndex(int j) {
        return paralEdgeIndex[j];
    }

    @Atomic
    public int numDirectedEdges() {
        return numDirectedEdges;
    }

    @Atomic
    public double numIntEdges() {
        return numIntEdges;
    }
}
