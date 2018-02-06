package graph;

import java.util.LinkedList;

public abstract class Node {
    public String name;
    public int weight;
    public LinkedList<Node> outEdges;
}
