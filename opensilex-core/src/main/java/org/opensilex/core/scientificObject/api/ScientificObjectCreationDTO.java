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
import java.util.Objects;

import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.geojson.GeoJsonObject;
import org.opensilex.core.experiment.api.ExperimentAPI;
import org.opensilex.core.ontology.api.RDFObjectDTO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author vmigot
 */
@JsonPropertyOrder({"uri", "rdf_type", "name", "experiment", "relations"})
public class ScientificObjectCreationDTO extends RDFObjectDTO {

    @NotNull
    @NotEmpty
    @JsonProperty("name")
    @ApiModelProperty(value = "Scientific object name", example = ScientificObjectAPI.SCIENTIFIC_OBJECT_EXAMPLE_NAME, required = true)
    protected String name;

    @ValidURI
    @JsonProperty("experiment")
    @ApiModelProperty(value = "Scientific object experiment URI", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI)
    private URI experiment;

    @Override
    @ValidURI
    @ApiModelProperty(value = "Scientific object URI", example = ScientificObjectAPI.SCIENTIFIC_OBJECT_EXAMPLE_URI)
    public URI getUri() {
        return uri;
    }

    @Override
    @ValidURI
    @NotNull
    @ApiModelProperty(value = "Scientific object type", example = ScientificObjectAPI.SCIENTIFIC_OBJECT_EXAMPLE_TYPE, required = true)
    public URI getType() {
        return type;
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

    public static ScientificObjectCsvDescriptionDTO fromString(String param) throws IOException {
        ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
        return mapper.readValue(param, ScientificObjectCsvDescriptionDTO.class);
    }

    public void toModel(ScientificObjectModel model) {
        model.setUri(getUri());
        model.setType(getType());
        model.setName(getName());
    }

    public ScientificObjectModel newModel() {
        ScientificObjectModel model = new ScientificObjectModel();
        toModel(model);

        return model;
    }


}
