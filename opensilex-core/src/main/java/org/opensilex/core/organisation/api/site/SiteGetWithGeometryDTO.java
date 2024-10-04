package org.opensilex.core.organisation.api.site;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.geojson.GeoJsonObject;
import org.opensilex.core.location.bll.LocationLogic;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.site.SiteModel;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.sparql.model.SPARQLResourceModel;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Get DTO for the site. The geometry is deduced from the address, if specified.
 *
 */
@JsonPropertyOrder({"uri", "rdf_type", "rdf_type_name", "publisher", "publication_date", "last_updated_date",
        "name", "facilities",  "geometry"})
public class SiteGetWithGeometryDTO extends SiteDTO {

    protected List<URI> facilities;

    @JsonProperty("geometry")
    protected GeoJsonObject geometry;

    @JsonProperty("publisher")
    protected UserGetDTO publisher;

    public List<URI> getFacilities() {
        return facilities;
    }

    public void setFacilities(List<URI> facilities) {
        this.facilities = facilities;
    }

    public GeoJsonObject getGeometry() {
        return geometry;
    }

    public void setGeometry(GeoJsonObject geometry) {
        this.geometry = geometry;
    }

    public UserGetDTO getPublisher() {
        return publisher;
    }

    public void setPublisher(UserGetDTO publisher) {
        this.publisher = publisher;
    }

    public void fromModel(SiteModel model, LocationObservationModel location) {
        super.fromModel(model);

        if (location != null) {
            try {
                setGeometry(LocationLogic.geometryToGeoJson(location.getLocation().getGeometry()));
            } catch (JsonProcessingException e) {
                throw new BadRequestException("Could not read GEOJSON geometry");
            }
        }

        List<FacilityModel> facilityModels = model.getFacilities();
        if (!Objects.isNull(facilityModels) && !facilityModels.isEmpty()) {
            List<URI> facilities = facilityModels.stream()
                    .map(SPARQLResourceModel::getUri)
                    .collect(Collectors.toList());
            setFacilities(facilities);
        }
    }
}

