/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.security.user;

import java.net.URI;
import org.apache.jena.vocabulary.SKOS;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.annotations.SPARQLResourceURI;
import org.opensilex.sparql.types.Label;

/**
 *
 * @author vincent
 */
@SPARQLResource(
        ontology = UserOntology.class,
        resource = "Group"
)
public class Group {
 
    @SPARQLResourceURI()
    private URI uri;
    
    @SPARQLProperty(
        ontology = SKOS.class,
        property = "prefLabel"
    )
    private Label label;
    
}
