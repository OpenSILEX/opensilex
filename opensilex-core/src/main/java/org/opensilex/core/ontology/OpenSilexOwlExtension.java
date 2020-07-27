/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.opensilex.sparql.utils.Ontology;

/**
 *
 * @author vince
 */
public class OpenSilexOwlExtension {

    public static final String DOMAIN = "http://www.opensilex.org/vocabulary/owl-extension";

    public static final String PREFIX = "oeso-owl";

    /**
     * The namespace of the vocabulary as a string
     */
    public static final String NS = DOMAIN + "#";

    /**
     * The namespace of the vocabulary as a string
     *
     * @return namespace as String
     * @see #NS
     */
    public static String getURI() {
        return NS;
    }

    /**
     * Vocabulary namespace
     */
    public static final Resource NAMESPACE = Ontology.resource(NS);
    
    public static final Resource ClassPropertyExtension = Ontology.resource(NS, "ClassPropertyExtension");

    public static final Property isAbstractClass = Ontology.property(NS, "isAbstractClass");
    
    public static final Property fromOwlClass = Ontology.property(NS, "fromOwlClass");
    public static final Property toOwlProperty = Ontology.property(NS, "toOwlProperty");
    public static final Property hasInputComponent = Ontology.property(NS, "hasInputComponent");
    public static final Property hasViewComponent = Ontology.property(NS, "hasViewComponent");
    public static final Property hasDisplayOrder = Ontology.property(NS, "hasDisplayOrder");
    
}