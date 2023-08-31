package org.opensilex.sparql.ontology;

import org.apache.jena.rdf.model.Property;
import org.opensilex.sparql.utils.Ontology;

public class OesoSparql {

    public static final String DOMAIN = "http://www.opensilex.org";

    public static final String PREFIX = "opensilex";

    public static final String NS = DOMAIN + "#";

    public static final Property shortLabel = Ontology.property(NS,"shortLabel");

}

