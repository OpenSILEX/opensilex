/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api.facitity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import java.net.URI;
import javax.validation.constraints.NotNull;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.core.organisation.dal.InfrastructureModel;
import org.opensilex.sparql.response.NamedResourceDTO;

/**
 * DTO representing JSON for getting facility
 *
 * @author vince
 */
@ApiModel
@JsonPropertyOrder({"uri", "rdf_type", "rdf_type_name", "name", "organisation"})
public class InfrastructureFacilityGetDTO extends NamedResourceDTO<InfrastructureFacilityModel> {

    @JsonProperty("rdf_type")
    protected URI type;

    @JsonProperty("rdf_type_name")
    protected String typeLabel;

    @JsonProperty("organisation")
    protected URI infrastructure;

    @NotNull
    public URI getInfrastructure() {
        return infrastructure;
    }

    public void setInfrastructure(URI infrastructure) {
        this.infrastructure = infrastructure;
    }

    @Override
    public void toModel(InfrastructureFacilityModel model) {
        super.toModel(model);
        InfrastructureModel infra = new InfrastructureModel();
        infra.setUri(getInfrastructure());
        model.setInfrastructure(infra);
    }

    @Override
    public void fromModel(InfrastructureFacilityModel model) {
        super.fromModel(model);
        if (model != null && model.getInfrastructure() != null) {
            setInfrastructure(model.getInfrastructure().getUri());
        }
    }

    @Override
    public InfrastructureFacilityModel newModelInstance() {
        return new InfrastructureFacilityModel();
    }

    public static InfrastructureFacilityGetDTO getDTOFromModel(InfrastructureFacilityModel model) {
        InfrastructureFacilityGetDTO dto = new InfrastructureFacilityGetDTO();
        dto.fromModel(model);

        return dto;
    }
}
