//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: maximilian.hart@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.geneticResourceGroup.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.core.geneticResource.dal.GeneticResourceModel;
import org.opensilex.core.geneticResourceGroup.dal.GeneticResourceGroupModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.response.NamedResourceDTO;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Maximilian HART
 */
public class GeneticResourceGroupGetDTO extends GeneticResourceGroupDTO {

    @JsonProperty("geneticResource_count")
    protected int geneticResourceCount;

    public int getGeneticResourceCount(){
        return this.geneticResourceCount;
    }

    public void setGeneticResourceCount(int geneticResourceCount){
        this.geneticResourceCount = geneticResourceCount;
    }

    public static GeneticResourceGroupGetDTO fromModel(GeneticResourceGroupModel model) {

        GeneticResourceGroupGetDTO dto = new GeneticResourceGroupGetDTO();

        dto.setUri(model.getUri());
        dto.setName(model.getName());
        dto.setDescription(model.getDescription());
        if (Objects.nonNull(model.getPublicationDate())) {
            dto.setPublicationDate(model.getPublicationDate());
        }

        if (Objects.nonNull(model.getLastUpdateDate())) {
            dto.setLastUpdatedDate(model.getLastUpdateDate());
        }

        return dto;
    }
}
