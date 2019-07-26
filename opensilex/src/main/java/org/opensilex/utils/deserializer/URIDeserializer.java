/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.utils.deserializer;

import java.net.URI;
import org.apache.jena.graph.Node;
import org.opensilex.utils.ontology.Ontology;

/**
 *
 * @author vincent
 */
public class URIDeserializer implements Deserializer<URI> {

    @Override
    public URI fromString(String value) throws Exception {
        return new URI(value);
    }

    @Override
    public Node getNode(Object value) throws Exception {
        return Ontology.nodeURI((URI) value);
    }
}