//******************************************************************************
//                                  Oeev.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 14 Nov. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.ontology;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Vocabulary definition from the OEEV ontology.
 * Follows Jena vocabulary java implementations.
 * @see http://agroportal.lirmm.fr/ontologies/OEEV
 * @see https://github.com/apache/jena/tree/master/jena-core/src/main/java/org/apache/jena/vocabulary
 * @update [Andréas Garcia] 5 Mar. 2019: Make the class follow the Jena 
 * vocabulary java implementation.
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class Oeev {
    public static final String NAMESPACE_STRING = "http://www.opensilex.org/vocabulary/oeev#";
    
    /**
     * @return URI of the ontology
     */
    public static String getURI() {
        return NAMESPACE_STRING;
    }

    /**
     * These will use ResourceFactory which creates Resource etc without a 
     * specific model.
     * This is safer for complex initialization paths.
     * @param uri
     * @return
     */
    protected static final Resource resource (String uri) { 
        return ResourceFactory.createResource (NAMESPACE_STRING + uri); 
    }

    /**
     * Gets a property object from a URI.
     * @param uri
     * @return
     */
    protected static final Property property (String uri) { 
        return ResourceFactory.createProperty(NAMESPACE_STRING, uri); 
    }

    /**
     * The RDF model that holds the vocabulary terms.
     */
    private static final Model MODEL = ModelFactory.createDefaultModel();
    
    /** 
     * The namespace of the vocabulary as a resource.
     */
    public static final Resource NAMESPACE = MODEL.createResource(NAMESPACE_STRING);
    
    public static final Resource Event = MODEL.createResource(NAMESPACE_STRING + "Event");

    public static final Property concerns = MODEL.createProperty(NAMESPACE + "concerns");
}