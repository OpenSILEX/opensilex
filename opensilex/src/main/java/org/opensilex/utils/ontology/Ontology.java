/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.utils.ontology;

import java.net.URI;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 *
 * @author vincent
 */
public abstract class Ontology {

    public static final Resource resource(String uri) {
        return ResourceFactory.createResource(uri);
    }
        
    public static final Resource resource(String namespace, String local) {
        return ResourceFactory.createResource(namespace + local);
    }

    public static final Property property(String uri) {
        return ResourceFactory.createProperty(uri);
    }
        
    public static final Property property(String namespace, String local) {
        return ResourceFactory.createProperty(namespace, local);
    }
    
    public static final Node nodeURI(URI uri) {
        return NodeFactory.createURI(uri.toString());
    }
}
