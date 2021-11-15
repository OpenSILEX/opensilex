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
import org.opensilex.core.organisation.dal.SiteModel;
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
@JsonPropertyOrder({"uri", "rdf_type", "rdf_type_name", "name", "organizations", "sites", "address"})
public class InfrastructureFacilityGetDTO extends InfrastructureFacilityDTO {

    @JsonProperty("organizations")
    protected List<NamedResourceDTO<InfrastructureModel>> infrastructures;

    @JsonProperty("site")
    protected NamedResourceDTO<SiteModel> site;

    @NotNull
    public List<NamedResourceDTO<InfrastructureModel>> getInfrastructures() {
        return infrastructures;
    }

    public void setInfrastructures(List<NamedResourceDTO<InfrastructureModel>> infrastructures) {
        this.infrastructures = infrastructures;
    }

    public NamedResourceDTO<SiteModel> getSite() {
        return site;
    }

    public void setSite(NamedResourceDTO<SiteModel> site) {
        this.site = site;
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

        if (getSite() != null) {
            SiteModel siteModel = new SiteModel();
            siteModel.setUri(getSite().getUri());
            model.setSite(siteModel);
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

        if (model.getSite() != null) {
            setSite(NamedResourceDTO.getDTOFromModel(model.getSite()));
        }
    }
}
