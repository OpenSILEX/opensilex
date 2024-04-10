/*******************************************************************************
 *                         JgraphtUtils.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.utils;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;

import java.util.*;

/**
 * @author rcolin
 */
public class JgraphtUtils {

    private JgraphtUtils(){
        // private constructor in order to prevent instantiation
    }

    /**
     *
     * @param graph the {@link Graph} on which performs this algorithm
     * @param ancestor the source vertex
     * @param descendant the destination vertex
     * @param maxPathLength maximum path length
     * @param <V> type of vertex
     * @param <E> type of edge
     *
     * @return the {@link Set} of vertex from any {@link GraphPath} between descendant and ancestor vertexes.
     * The ancestor is included (but not the descendant) into this set if these two vertexes are connected, else the returned set if empty.
     *
     * @see AllDirectedPaths#getAllPaths(Object, Object, boolean, Integer)
     */
    public static <V, E> LinkedHashSet<V> getVertexesFromAncestor(Graph<V,E> graph, V ancestor, V descendant, int maxPathLength) {

        Objects.requireNonNull(graph);
        Objects.requireNonNull(ancestor);
        Objects.requireNonNull(descendant);

        if(maxPathLength <= 0){
            throw new IllegalArgumentException("maxPathLength must be strictly positive : "+maxPathLength);
        }

        if(! graph.containsVertex(descendant) || ! graph.containsVertex(ancestor)){
            return new LinkedHashSet<V>(0);
        }

        AllDirectedPaths<V, E> allPaths = new AllDirectedPaths<>(graph);
        List<GraphPath<V, E>> pathList = allPaths.getAllPaths(ancestor, descendant, true, maxPathLength);

        if (pathList.isEmpty()) {
            return new LinkedHashSet<V>(0);
        }

        LinkedHashSet<V> ancestors = new LinkedHashSet<V>();
        for (GraphPath<V, E> path : pathList) {
            ancestors.addAll(path.getVertexList());
        }
        ancestors.remove(descendant);
        return ancestors;
    }
}
