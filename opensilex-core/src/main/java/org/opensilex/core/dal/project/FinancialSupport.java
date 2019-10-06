/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.dal.project;

import java.net.URI;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.annotations.SPARQLResourceURI;

/**
 * Financial support of a project.
 * @author Morgane Vidal
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "FinancialSupport"
)
public class FinancialSupport {
    //URI of the financial support
    @SPARQLResourceURI()
    private URI uri;
    
    //Label of the financial support
    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label"
    )
    private String label;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
