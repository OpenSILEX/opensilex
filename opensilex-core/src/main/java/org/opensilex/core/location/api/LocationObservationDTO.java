/*
 * *****************************************************************************
 *                         LocationObservationDTO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 02/10/2024 11:11
 * Contact: alexia.chiavarino@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 *
 *
 * *****************************************************************************
 *
 */

package org.opensilex.core.location.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.geojson.GeoJsonObject;
import org.opensilex.core.location.bll.LocationLogic;
import org.opensilex.core.location.dal.LocationModel;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

public class LocationObservationDTO {

    private GeoJsonObject geojson;
    protected URI featureOfInterest;

    protected String label;

    private Instant startDate;
    protected URI from;

    protected URI to;

    @JsonProperty("x")
    private String x;

    @JsonProperty("y")
    private String y;

    @JsonProperty("z")
    private String z;

    @JsonProperty("text")
    private String textualPosition;

    private Instant endDate;

    public URI getFeatureOfInterest() {
        return featureOfInterest;
    }

    public void setFeatureOfInterest(URI featureOfInterest) {
        this.featureOfInterest = featureOfInterest;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public GeoJsonObject getGeojson() {
        return geojson;
    }

    public void setGeojson(GeoJsonObject geojson) {
        this.geojson = geojson;
    }

    public URI getFrom() {
        return from;
    }

    public void setFrom(URI from) {
        this.from = from;
    }

    public URI getTo() {
        return to;
    }

    public void setTo(URI to) {
        this.to = to;
    }

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }

    public String getZ() {
        return z;
    }

    public void setZ(String z) {
        this.z = z;
    }

    public String getTextualPosition() {
        return textualPosition;
    }

    public void setTextualPosition(String textualPosition) {
        this.textualPosition = textualPosition;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    private void toModel(LocationObservationModel model) {

        model.setFeatureOfInterest(getFeatureOfInterest());

        try {
            LocationModel location = LocationLogic.buildLocationModel(
                    Objects.nonNull(getGeojson()) ? LocationLogic.geoJsonToGeometry(geojson) : null,
                    from,
                    to,
                    x,
                    y,
                    z,
                    textualPosition
            );
            model.setLocation(location);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        if (Objects.nonNull(getStartDate())) {
            model.setStartDate(getStartDate());
        }
        if (Objects.nonNull(getEndDate())) {
            model.setEndDate(getEndDate());
        }
    }

    public LocationObservationModel newModel() {
        LocationObservationModel instance = new LocationObservationModel();
        toModel(instance);

        return instance;
    }

    private void fromModel(LocationObservationModel model) {

        setFeatureOfInterest(model.getFeatureOfInterest());

        //A bunch of stuff gets set via the models location, so one check here to avoid null pointer exceptions
        if (model.getLocation() != null) {
            if(model.getLocation().getGeometry() != null){
                try {
                    setGeojson(LocationLogic.geometryToGeoJson(model.getLocation().getGeometry()));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            if (Objects.nonNull(model.getLocation().getFrom())) {
                setFrom(model.getLocation().getFrom());
            }
            if (Objects.nonNull(model.getLocation().getTo())) {
                setTo(model.getLocation().getTo());
            }
            if (Objects.nonNull(model.getLocation().getY())) {
                setY(model.getLocation().getY());
            }
            if (Objects.nonNull(model.getLocation().getX())) {
                setX(model.getLocation().getX());
            }
            if (Objects.nonNull(model.getLocation().getZ())) {
                setZ(model.getLocation().getZ());
            }
            if (Objects.nonNull(model.getLocation().getTextualPosition())) {
                setTextualPosition(model.getLocation().getTextualPosition());
            }
        }
        if (Objects.nonNull(model.getStartDate())) {
            setStartDate(model.getStartDate());
        }
        if (Objects.nonNull(model.getEndDate())) {
            setEndDate(model.getEndDate());
        }
    }

    public static LocationObservationDTO getDTOFromModel(LocationObservationModel model) {
        LocationObservationDTO dto = new LocationObservationDTO();
        dto.fromModel(model);

        return dto;
    }

    public static LocationObservationDTO getDTOFromModel(SPARQLNamedResourceModel model, LocationObservationModel location) {
        LocationObservationDTO dto = new LocationObservationDTO();
        dto.fromModel(location);
        dto.setLabel(model.getName());

        return dto;
    }
}
