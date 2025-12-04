//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: maximilian.hart@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.geneticResourceGroup.dal;

import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.geneticResource.dal.GeneticResourceModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

import java.util.List;

/**
 * @author Maximilian HART
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "GeneticResourceGroup",
        graph = GeneticResourceGroupModel.GRAPH
)
public class GeneticResourceGroupModel extends SPARQLNamedResourceModel<GeneticResourceGroupModel> {

    public static final String GRAPH = "geneticResourceGroup";

    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "description"
    )
    private String description;
    public static final String DESCRIPTION_FIELD = "description";

    
    @SPARQLProperty(
            ontology = RDFS.class,
            property = "member"
    )
    private List<GeneticResourceModel> geneticResourceList;
    public static final String GENETIC_RESOURCE_LIST_FIELD = "geneticResourceList";
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<GeneticResourceModel> getGeneticResourceList() {
        return geneticResourceList;
    }
    
    public void setGeneticResourceList(List<GeneticResourceModel> geneticResource) {
        this.geneticResourceList = geneticResource;
    }
}
