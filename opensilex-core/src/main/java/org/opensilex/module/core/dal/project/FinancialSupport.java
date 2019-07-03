/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.dal.project;

import java.net.URI;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.module.core.ontology.Oeso;
import org.opensilex.module.core.service.sparql.annotations.SPARQLProperty;
import org.opensilex.module.core.service.sparql.annotations.SPARQLResource;
import org.opensilex.module.core.service.sparql.annotations.SPARQLResourceURI;

/**
 * Financial support of a project.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
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
