package org.opensilex.core.data.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.media.Schema;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import java.util.List;

@Schema
public class CriteriaDTO {

    public static CriteriaDTO fromString(String serialized) {
        try {
            return ObjectMapperContextResolver.getObjectMapper().readValue(serialized, CriteriaDTO.class);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Could not read CriteriaDTO");
        }
    }

    @JsonProperty("criteria_list")
    @Schema(description = "list of criteria to combine with the given logic", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<SingleCriteriaDTO> criteriaList;

    public List<SingleCriteriaDTO> getCriteriaList() {
        return criteriaList;
    }

    public void setCriteriaList(List<SingleCriteriaDTO> criteriaList) {
        this.criteriaList = criteriaList;
    }
}
