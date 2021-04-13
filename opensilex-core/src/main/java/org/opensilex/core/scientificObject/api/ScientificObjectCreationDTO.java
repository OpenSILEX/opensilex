/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import org.geojson.GeoJsonObject;
import org.opensilex.core.ontology.api.RDFObjectDTO;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author vmigot
 */
@JsonPropertyOrder({"uri", "rdf_type", "name", "experiment", "relations", "geometry"})
public class ScientificObjectCreationDTO extends RDFObjectDTO {

    @ValidURI
    protected URI uri;

    @ValidURI
    @NotNull
    @JsonProperty("rdf_type")
    @ApiModelProperty(value = "Scientific object type", example = ScientificObjectAPI.SCIENTIFIC_OBJECT_EXAMPLE_TYPE)
    protected URI type;

    @ApiModelProperty(value = "Scientific object name", example = ScientificObjectAPI.SCIENTIFIC_OBJECT_EXAMPLE_NAME)
    protected String name;

    @ValidURI
    @ApiModelProperty(value = "Scientific object experiment URI")
    private URI experiment;

    /**
     * geometry of the Geospatial
     */
    @ApiModelProperty(value = "The geographical coordinates of the Geospatial", example = "{'type':'Polygon','coordinates':[[[3.97167246,43.61328981], [3.97171243,43.61332417],[3.9717427,43.61330558],[3.97170272,43.61327122], [3.97167246,43.61328981],[3.97167246,43.61328981]]]}")
    private GeoJsonObject geometry;

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    public URI getExperiment() {
        return experiment;
    }

    public void setExperiment(URI experiment) {
        this.experiment = experiment;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GeoJsonObject getGeometry() {
        return geometry;
    }

    public void setGeometry(GeoJsonObject geometry) {
        this.geometry = geometry;
    }

    public static ScientificObjectCsvDescriptionDTO fromString(String param) throws IOException {
        ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
        return mapper.readValue(param, ScientificObjectCsvDescriptionDTO.class);
    }

}
