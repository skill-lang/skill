package de.ust.skill.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * sort a list of declarations in topological order maintaining its internal order
 * 
 * @note this is required by projects, as removing edges can change order
 * @author Timm Felden
 */
public class TopologicalSort {
    private final HashMap<Declaration, ArrayList<Declaration>> edges;
    private final HashSet<Declaration> marked;
    // @note we do not need temporary nodes, because we already know that the graph is a DAG

    final LinkedList<Declaration> result;

    TopologicalSort(List<Declaration> nodes) {
        
        edges = new HashMap<>();
        for (Declaration n : nodes) {
            edges.put(n, new ArrayList<>());
        }

        for (Declaration n : nodes) {
            if (n instanceof WithInheritance) {
                edges.get(n).addAll(((WithInheritance) n).getSubTypes());
            } else if (n instanceof Typedef) {
                ArrayList<Declaration> s = edges.get(((Typedef) n).getTarget());
                if (null != s) {
                    s.add(n);
                }
            }
        }

        // L ← Empty list that will contain the sorted nodes
        result = new LinkedList<>();
        marked = new HashSet<>();
 

        // while there are unmarked nodes do
        // select an unmarked node n
        // visit(n)
        // @note we do not use the unmarked set, because we known which nodes are roots of the DAG and the resulting
        // order
        // has to be created in that way
        nodes.sort((l, r) -> l.name.compareTo(r.name));
        
        for (Declaration n : nodes)
            visit(n);
    }

    private void visit(Declaration n) {

        // if n is not marked (i.e. has not been visited yet) then
        if (!marked.contains(n)) {

            // for each node m with an edge from n to m do
            // visit(m)
            for (Declaration e : edges.get(n))
                visit(e);

            // mark n permanently
            marked.add(n);
            // add n to head of L
            result.addFirst(n);
        }
    };
}
