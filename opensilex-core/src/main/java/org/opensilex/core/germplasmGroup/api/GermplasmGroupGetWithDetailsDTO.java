package org.opensilex.core.germplasmGroup.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.core.germplasm.api.GermplasmGetAllDTO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.germplasmGroup.dal.GermplasmGroupModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GermplasmGroupGetWithDetailsDTO extends GermplasmGroupGetDTO{

    @JsonProperty("germplasm_list")
    protected List<GermplasmGetAllDTO> germplasmList = new ArrayList<>();

    public List<GermplasmGetAllDTO> getGermplasmList() {
        return germplasmList;
    }

    public void setGermplasmList(List<GermplasmGetAllDTO> germplasmList) {
        this.germplasmList = germplasmList;
    }

    public static GermplasmGroupGetWithDetailsDTO fromModel(GermplasmGroupModel model) {

        GermplasmGroupGetWithDetailsDTO dto = new GermplasmGroupGetWithDetailsDTO();

        dto.setUri(model.getUri());
        dto.setName(model.getName());
        dto.setDescription(model.getDescription());
        dto.setGermplasmList(model.getGermplasmList().stream().map((GermplasmGetAllDTO::fromModel)).collect(Collectors.toList()));

        return dto;
    }
}
