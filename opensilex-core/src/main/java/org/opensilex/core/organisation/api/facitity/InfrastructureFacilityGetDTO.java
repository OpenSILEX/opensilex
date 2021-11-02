/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api.facitity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.core.organisation.dal.InfrastructureModel;
import org.opensilex.sparql.response.NamedResourceDTO;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO representing JSON for getting facility
 *
 * @author vince
 */
@ApiModel
@JsonPropertyOrder({"uri", "rdf_type", "rdf_type_name", "name", "organizations", "address"})
public class InfrastructureFacilityGetDTO extends InfrastructureFacilityDTO {

    @JsonProperty("rdf_type_name")
    protected String typeLabel;

    @JsonProperty("organizations")
    protected List<NamedResourceDTO<InfrastructureModel>> infrastructures;

    protected String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public void setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
    }

    @NotNull
    public List<NamedResourceDTO<InfrastructureModel>> getInfrastructures() {
        return infrastructures;
    }

    public void setInfrastructures(List<NamedResourceDTO<InfrastructureModel>> infrastructures) {
        this.infrastructures = infrastructures;
    }

    @Override
    public void toModel(InfrastructureFacilityModel model) {
        super.toModel(model);

        if (getInfrastructures() != null) {
            List<InfrastructureModel> infrastructureModels = new ArrayList<>();
            getInfrastructures().forEach(infrastructure -> {
                InfrastructureModel infrastructureModel = new InfrastructureModel();
                infrastructureModel.setUri(infrastructure.getUri());
                infrastructureModels.add(infrastructureModel);
            });
            model.setInfrastructures(infrastructureModels);
        }
    }

    public void fromModel(InfrastructureFacilityModel model) {
        super.fromModel(model);

        if (model.getInfrastructures() != null) {
            setInfrastructures(model.getInfrastructures()
                    .stream()
                    .map(infrastructureModel ->
                            (NamedResourceDTO<InfrastructureModel>)NamedResourceDTO.getDTOFromModel(infrastructureModel))
                    .collect(Collectors.toList()));
        }
    }
}
