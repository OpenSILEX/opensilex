/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.deserializer;

import java.net.URI;
import org.apache.jena.graph.Node;
import org.opensilex.sparql.utils.Ontology;

/**
 *
 * @author vincent
 */
public class URIDeserializer implements SPARQLDeserializer<URI> {

    @Override
    public URI fromString(String value) throws Exception {
        return new URI(value);
    }

    @Override
    public Node getNode(Object value) throws Exception {
        return Ontology.nodeURI((URI) value);
    }
}