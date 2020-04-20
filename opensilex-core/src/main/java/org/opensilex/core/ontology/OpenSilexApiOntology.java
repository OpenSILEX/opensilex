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
public class OpenSilexApiOntology {

    public static final String DOMAIN = "http://www.opensilex.org/vocabulary/opensilex-api";

    public static final String PREFIX = "oxapi";

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

    // ---- GENERIC PROPERTIES MAPPING ----
    public static final Resource PropertyMapping = Ontology.property(NS, "PropertyMapping");

    public static final Property propertyMapping = Ontology.property(NS, "propertyMapping");
    public static final Property about = Ontology.property(NS, "about");
    public static final Property type = Ontology.property(NS, "type");
    public static final Property isList = Ontology.property(NS, "isList");
    public static final Property isRequired = Ontology.property(NS, "isRequired");
    public static final Property isInverse = Ontology.property(NS, "isInverse");
    public static final Property ignoreUpdateIfNull = Ontology.property(NS, "ignoreUpdateIfNull");
    public static final Property cascadeDelete = Ontology.property(NS, "cascadeDelete");
    public static final Property order = Ontology.property(NS, "order");
    public static final Property viewComponent = Ontology.property(NS, "viewComponent");
    public static final Property editComponent = Ontology.property(NS, "editComponent");

}
