/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.dal.user;

import java.net.URI;
import org.apache.jena.vocabulary.SKOS;
import org.opensilex.module.core.service.sparql.annotations.SPARQLProperty;
import org.opensilex.module.core.service.sparql.annotations.SPARQLResource;
import org.opensilex.module.core.service.sparql.annotations.SPARQLResourceURI;
import org.opensilex.module.core.service.sparql.types.Label;
import org.opensilex.module.core.ontology.Oeso;

/**
 *
 * @author vincent
 */
@SPARQLResource(
        ontology = Oeso.class,
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
