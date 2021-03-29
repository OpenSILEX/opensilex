package org.opensilex.core.ontology;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.opensilex.sparql.utils.Ontology;

public class Oeev {

    public static final String DOMAIN = "http://www.opensilex.org/vocabulary/oeev";

    public static final String PREFIX = "oeev";

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


    // Event types

    public static final Resource Event =  Ontology.resource(NS, "Event");

    public static final Resource Move =  Ontology.resource(NS, "Move");

    public static final Property concerns = Ontology.property(NS, "concerns");

    public static final Property isInstant = Ontology.property(NS, "isInstant");

    public static final Property to = Ontology.property(NS, "to");

    public static final Property from = Ontology.property(NS, "from");

}