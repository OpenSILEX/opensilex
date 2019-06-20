/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.ontology;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.opensilex.utils.ontology.Ontology;

/**
 *
 * @author vincent
 */
public class Oeso {
    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://www.opensilex.org/oeso#";

    /** <p>The namespace of the vocabulary as a string</p>
     * @return namespace as String
     * @see #NS */
    public static String getURI() {return NS;}

    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = Ontology.resource(NS );
    
    // TODO
    public static final Property hasGroup = Ontology.property(NS, "hasGroup");

    // TODO
    public static final Resource R = Ontology.resource(NS, "R");
    
}
