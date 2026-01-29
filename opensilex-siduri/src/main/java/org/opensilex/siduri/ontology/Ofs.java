package org.opensilex.core.ontology;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.opensilex.sparql.utils.Ontology;

/**
 * Ontology for Siduri  
**/


public class Ofs {

    public static final String DOMAIN = "http://opendata.inrae.fr/ofs";
    public static final String PREFIX = "ofs";


    /**
     * The namespace of the vocabulary as a string
     */
    public static final String NS = DOMAIN + "#";

    
    public static String getURI() {
        return NS;
    }
    /**
     * namespace
     */

    public static final Resource Strain = Ontology.resource(NS, "Strain");
    public static final Property fromStrain = Ontology.property(NS, "fromStrain");

    // public static final Property isFdF = Ontology.property(NS, "isFdF");

    // public static final Resource ResearchProductFamily = Ontology.resource(NS, "ResearchProductFamily");
    // public static final Property hasResearchProductFamily = Ontology.property(NS, "hasResearchProductFamily");


}
