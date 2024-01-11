/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api.facility;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.ApiModel;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.site.SiteModel;
import org.opensilex.core.variablesGroup.dal.VariablesGroupModel;
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
@JsonPropertyOrder({"uri", "publisher", "publication_date", "last_updated_date", "rdf_type", "rdf_type_name", "name", "organizations", "sites", "address", "variableGroups"})
public class FacilityGetDTO extends FacilityDTO {

    @JsonProperty("organizations")
    protected List<NamedResourceDTO<OrganizationModel>> organizations;

    @JsonProperty("sites")
    protected List<NamedResourceDTO<SiteModel>> sites;

    @JsonProperty("variableGroups")
    protected List<NamedResourceDTO<VariablesGroupModel>> variablesGroups;

    @NotNull
    public List<NamedResourceDTO<OrganizationModel>> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(List<NamedResourceDTO<OrganizationModel>> organizations) {
        this.organizations = organizations;
    }

    public List<NamedResourceDTO<SiteModel>> getSites() {
        return sites;
    }

    public void setSites(List<NamedResourceDTO<SiteModel>> sites) {
        this.sites = sites;
    }

    public List<NamedResourceDTO<VariablesGroupModel>> getVariablesGroups() {
        return variablesGroups;
    }

    public void setVariablesGroups(List<NamedResourceDTO<VariablesGroupModel>> variablesGroups) {
        this.variablesGroups = variablesGroups;
    }

    @Override
    public void toModel(FacilityModel model) {
        super.toModel(model);

        if (getSites() != null) {
            List<SiteModel> siteModels = new ArrayList<>();
            getSites().forEach(site -> {
                SiteModel siteModel = new SiteModel();
                siteModel.setUri(site.getUri());
                siteModels.add(siteModel);
            });
            model.setSites(siteModels);
        }

        if (getVariablesGroups() != null) {
            List<VariablesGroupModel> variablesGroupModels = new ArrayList<>();
            getVariablesGroups().forEach(group -> {
                VariablesGroupModel groupModel = new VariablesGroupModel();
                groupModel.setUri(group.getUri());
                variablesGroupModels.add(groupModel);
            });
            model.setVariableGroups(variablesGroupModels);
        }
    }

    public void fromModel(FacilityModel model) {
        super.fromModel(model);

        if (model.getOrganizations() != null) {
            setOrganizations(model.getOrganizations()
                    .stream()
                    .map(organizationModel ->
                            (NamedResourceDTO<OrganizationModel>)NamedResourceDTO.getDTOFromModel(organizationModel))
                    .collect(Collectors.toList()));
        }

        if (model.getSites() != null) {
            setSites(model.getSites().stream()
                    .map(siteModel ->
                            (NamedResourceDTO<SiteModel>)NamedResourceDTO.getDTOFromModel(siteModel))
                    .collect(Collectors.toList()));
        }

        if (model.getVariableGroups() != null) {
            setVariablesGroups(model.getVariableGroups().stream()
                    .map(groupModel ->
                            (NamedResourceDTO<VariablesGroupModel>)NamedResourceDTO.getDTOFromModel(groupModel))
                    .collect(Collectors.toList()));
        }
    }

    public void fromModelWithGeospatialInfo(FacilityModel facilityModel, GeospatialModel geospatialModel) throws JsonProcessingException {
        fromModel(facilityModel);
        fromGeospatialModel(geospatialModel);
    }

    public void fromGeospatialModel(GeospatialModel geospatialModel) throws JsonProcessingException {
        if (geospatialModel != null) {
            setGeometry(GeospatialDAO.geometryToGeoJson(geospatialModel.getGeometry()));
        }
    }
}