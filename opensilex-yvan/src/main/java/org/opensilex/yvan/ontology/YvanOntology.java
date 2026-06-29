package org.opensilex.yvan.ontology;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.opensilex.sparql.utils.Ontology;

public class YvanOntology {

    public static final String DOMAIN = "http://www.yvan.extension.org";

    public static final String PREFIX = "yvan";

    /**
     * The namespace of the vocabulary as a string
     */
    public static final String NS = DOMAIN + "#";

    public static final Resource SpiderMutagen = Ontology.resource(NS, "SpiderMutagen");
    public static final Property hasExperiment = Ontology.property(NS, "hasExperiment");
    public static final Property pawsNumber = Ontology.property(NS, "pawsNumber");
}
