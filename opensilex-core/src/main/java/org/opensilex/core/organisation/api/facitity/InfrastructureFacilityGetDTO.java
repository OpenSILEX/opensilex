/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api.facitity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.ApiModel;
import org.geojson.GeoJsonObject;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.geospatial.dal.GeospatialModel;
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
@JsonPropertyOrder({"uri", "rdf_type", "rdf_type_name", "name", "organizations", "sites", "address", "geometry"})
public class InfrastructureFacilityGetDTO extends InfrastructureFacilityDTO {

    @JsonProperty("organizations")
    protected List<NamedResourceDTO<InfrastructureModel>> infrastructures;

    @JsonProperty("sites")
    protected List<NamedResourceDTO<SiteModel>> sites;

    @JsonProperty("geometry")
    protected GeoJsonObject geometry;

    @NotNull
    public List<NamedResourceDTO<InfrastructureModel>> getInfrastructures() {
        return infrastructures;
    }

    public void setInfrastructures(List<NamedResourceDTO<InfrastructureModel>> infrastructures) {
        this.infrastructures = infrastructures;
    }

    public List<NamedResourceDTO<SiteModel>> getSites() {
        return sites;
    }

    public void setSites(List<NamedResourceDTO<SiteModel>> sites) {
        this.sites = sites;
    }

    public GeoJsonObject getGeometry() {
        return geometry;
    }

    public void setGeometry(GeoJsonObject geometry) {
        this.geometry = geometry;
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

        if (getSites() != null) {
            List<SiteModel> siteModels = new ArrayList<>();
            getSites().forEach(site -> {
                SiteModel siteModel = new SiteModel();
                siteModel.setUri(site.getUri());
                siteModels.add(siteModel);
            });
            model.setSites(siteModels);
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

        if (model.getSites() != null) {
            setSites(model.getSites().stream()
                    .map(siteModel -> (NamedResourceDTO<SiteModel>)NamedResourceDTO.getDTOFromModel(siteModel))
                    .collect(Collectors.toList()));
        }
    }

    public void fromModelWithGeospatialInfo(InfrastructureFacilityModel facilityModel, GeospatialModel geospatialModel) throws JsonProcessingException {
        fromModel(facilityModel);
        fromGeospatialModel(geospatialModel);
    }

    public void fromGeospatialModel(GeospatialModel geospatialModel) throws JsonProcessingException {
        if (geospatialModel != null) {
            setGeometry(GeospatialDAO.geometryToGeoJson(geospatialModel.getGeometry()));
        }
    }
}
