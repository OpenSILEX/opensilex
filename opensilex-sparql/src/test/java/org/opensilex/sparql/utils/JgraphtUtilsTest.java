package org.opensilex.sparql.utils;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Set;

/**
 * @author rcolin
 */
public class JgraphtUtilsTest {

    @Test
    public void testGetVertexesFromAncestor(){

        Graph<String, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);
        graph.addVertex("root");
        graph.addVertex("c0");
        graph.addVertex("c01");
        graph.addVertex("c02");
        graph.addVertex("c1");
        graph.addVertex("c2");
        graph.addVertex("c3");
        graph.addVertex("c31");

        graph.addEdge("root","c0");
        graph.addEdge("root","c01");
        graph.addEdge("root","c02");

        graph.addEdge("c0","c1");
        graph.addEdge("c01","c1");

        graph.addEdge("c1","c2");
        graph.addEdge("c02","c2");

        graph.addEdge("c2","c3");
        graph.addEdge("c2","c31");

        Set<String> ancestors = JgraphtUtils.getVertexesFromAncestor(graph,"root","c2",100);
        Assert.assertTrue(ancestors.containsAll(Arrays.asList("root", "c0", "c01", "c1", "c02")));
    }
    
}