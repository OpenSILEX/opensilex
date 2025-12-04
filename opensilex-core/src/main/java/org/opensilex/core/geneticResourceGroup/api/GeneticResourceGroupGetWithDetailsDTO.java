package org.opensilex.core.geneticResourceGroup.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.core.geneticResource.api.GeneticResourceGetAllDTO;
import org.opensilex.core.geneticResource.dal.GeneticResourceModel;
import org.opensilex.core.geneticResourceGroup.dal.GeneticResourceGroupModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GeneticResourceGroupGetWithDetailsDTO extends GeneticResourceGroupGetDTO{

    @JsonProperty("geneticResource_list")
    protected List<GeneticResourceGetAllDTO> geneticResourceList = new ArrayList<>();

    public List<GeneticResourceGetAllDTO> getGeneticResourceList() {
        return geneticResourceList;
    }

    public void setGeneticResourceList(List<GeneticResourceGetAllDTO> geneticResourceList) {
        this.geneticResourceList = geneticResourceList;
    }

    public static GeneticResourceGroupGetWithDetailsDTO fromModel(GeneticResourceGroupModel model) {

        GeneticResourceGroupGetWithDetailsDTO dto = new GeneticResourceGroupGetWithDetailsDTO();

        dto.setUri(model.getUri());
        dto.setName(model.getName());
        dto.setDescription(model.getDescription());
        dto.setGeneticResourceList(model.getGeneticResourceList().stream().map((GeneticResourceGetAllDTO::fromModel)).collect(Collectors.toList()));

        return dto;
    }
}
