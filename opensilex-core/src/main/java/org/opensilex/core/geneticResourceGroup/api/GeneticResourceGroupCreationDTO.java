//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: maximilian.hart@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.geneticResourceGroup.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.opensilex.core.geneticResource.dal.GeneticResourceModel;
import org.opensilex.core.geneticResourceGroup.dal.GeneticResourceGroupModel;
import org.opensilex.server.rest.validation.Required;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Maximilian HART
 */
@JsonPropertyOrder({"uri", "name", "description","geneticResource_list"})
public class GeneticResourceGroupCreationDTO extends GeneticResourceGroupDTO {
    
    @JsonProperty("geneticResource_list")
    protected List<URI> geneticResourceList = new ArrayList<>();

    @Override
    @Required
    public String getName() {
        return super.getName();
    }

    public List<URI> getGeneticResourceList() {
        return geneticResourceList;
    }
    
    public void setGeneticResourceList(List<URI> geneticResourceList) {
        this.geneticResourceList = geneticResourceList;
    }
    
    public GeneticResourceGroupModel newModel(){
        
        GeneticResourceGroupModel model = new GeneticResourceGroupModel();
        
        model.setUri(getUri());
        model.setName(name);
        model.setDescription(description);

        List<GeneticResourceModel> geneticResourceList = this.geneticResourceList.stream().map((URI uri) -> {
            GeneticResourceModel geneticResourceModel = new GeneticResourceModel();
            geneticResourceModel.setUri(uri);
            return geneticResourceModel;
        }).collect(Collectors.toList());
        model.setGeneticResourceList(geneticResourceList);
        
        return model;
    }
}
