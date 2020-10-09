/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;

import io.swagger.annotations.ApiModelProperty;
import org.geojson.GeoJsonObject;
import org.opensilex.core.ontology.api.RDFObjectDTO;

/**
 *
 * @author vmigot
 */
public class ScientificObjectDescriptionDTO extends RDFObjectDTO {

    private URI experiment;

    public URI getExperiment() {
        return experiment;
    }

    public void setExperiment(URI experiment) {
        this.experiment = experiment;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * geometry of the Geospatial
     */
    @ApiModelProperty(value = "The geographical coordinates of the Geospatial", example = "{'type':'Polygon','coordinates':[[[3.97167246,43.61328981], [3.97171243,43.61332417],[3.9717427,43.61330558],[3.97170272,43.61327122], [3.97167246,43.61328981],[3.97167246,43.61328981]]]}")
    protected GeoJsonObject geometry;

    public GeoJsonObject getGeometry() {
        return geometry;
    }

    public void setGeometry(GeoJsonObject geometry) {
        this.geometry = geometry;
    }

    public static ScientificObjectCsvDescriptionDTO fromString(String param) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(param, ScientificObjectCsvDescriptionDTO.class);
    }

}
