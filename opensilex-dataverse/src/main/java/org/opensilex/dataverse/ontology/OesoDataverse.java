package org.opensilex.dataverse.ontology;

import org.apache.jena.rdf.model.Resource;
import org.opensilex.sparql.utils.Ontology;

public class OesoDataverse {

    public static final String DOMAIN = "http://www.opensilex.org/vocabulary/oeso-dataverse";

    public static final String PREFIX = "vocabulary-dataverse";

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

    // ---- DATAVERSE DATASET ----
    public static final Resource DataverseDataset = Ontology.resource(NS, "DataverseDataset");
}
