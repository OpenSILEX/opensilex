/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensilex.service.model;

import opensilex.service.resource.ProjectResourceService;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 *
 * @author vincent
 */
@SPARQLResource(
        ontology = ProjectResourceService.class,
        resource = "FinancialFunding"
)
public class FinancialFunding extends SPARQLResourceModel {

    /**
     * The rdfs:label of the instance.
     *
     * @example International
     */
    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label",
            required = true
    )
    protected String label;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

}
