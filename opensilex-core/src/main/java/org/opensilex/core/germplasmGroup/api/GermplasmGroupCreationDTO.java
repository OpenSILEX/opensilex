//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: maximilian.hart@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.germplasmGroup.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.germplasmGroup.dal.GermplasmGroupModel;
import org.opensilex.server.rest.validation.Required;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Maximilian HART
 */
@JsonPropertyOrder({"uri", "name", "description","germplasm_list"})
public class GermplasmGroupCreationDTO extends GermplasmGroupDTO {
    
    @JsonProperty("germplasm_list")
    protected List<URI> germplasmList = new ArrayList<>();

    @Override
    @Required
    public String getName() {
        return super.getName();
    }

    public List<URI> getGermplasmList() {
        return germplasmList;
    }
    
    public void setGermplasmList(List<URI> germplasmList) {
        this.germplasmList = germplasmList;
    }
    
    public GermplasmGroupModel newModel(){
        
        GermplasmGroupModel model = new GermplasmGroupModel();
        
        model.setUri(getUri());
        model.setName(name);
        model.setDescription(description);

        List<GermplasmModel> germplasmList = this.germplasmList.stream().map((URI uri) -> {
            GermplasmModel germplasmModel = new GermplasmModel();
            germplasmModel.setUri(uri);
            return germplasmModel;
        }).collect(Collectors.toList());
        model.setGermplasmList(germplasmList);
        
        return model;
    }
}
