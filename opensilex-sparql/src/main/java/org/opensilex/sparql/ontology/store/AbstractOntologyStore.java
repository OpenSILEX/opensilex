/*******************************************************************************
 *                         AbstractOntologyStore.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.ontology.store;


import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractOntologyStore implements OntologyStore {

    static final int MAX_GRAPH_PATH_LENGTH = 20;

    Graph<String, DefaultEdge> classesGraph;

    protected AbstractOntologyStore() {
        classesGraph = new SimpleDirectedGraph<>(DefaultEdge.class);
    }

    void addEdgeBetweenParentAndClass(String parentURI, String classURI) {
        classesGraph.addVertex(parentURI);
        classesGraph.addVertex(classURI);
        classesGraph.addEdge(parentURI, classURI);
    }

    Set<String> getVertexesFromAncestor(String classURI, String parentURI) {

        AllDirectedPaths<String, DefaultEdge> allPaths = new AllDirectedPaths<>(classesGraph);
        List<GraphPath<String, DefaultEdge>> pathList = allPaths.getAllPaths(parentURI, classURI, true, MAX_GRAPH_PATH_LENGTH);

        if (pathList.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> ancestors = new HashSet<>();
        for (GraphPath<String, DefaultEdge> path : pathList) {
            ancestors.addAll(path.getVertexList());
        }
        ancestors.remove(classURI);
        return ancestors;
    }
}
