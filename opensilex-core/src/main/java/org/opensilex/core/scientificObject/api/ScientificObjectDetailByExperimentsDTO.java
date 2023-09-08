/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;

import java.net.URI;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.experiment.dal.ExperimentModel;

import static org.opensilex.core.geospatial.dal.GeospatialDAO.geometryToGeoJson;

/**
 *
 * @author vmigot
 */
@JsonPropertyOrder({"uri", "publisher", "publication_date", "last_updated_date", "rdf_type", "rdf_type_name", "name", "parent", "parent_name", "experiment", "experiment_name", "factor_level", "relations", "geometry"})
public class ScientificObjectDetailByExperimentsDTO extends ScientificObjectDetailDTO {

    @ApiModelProperty(value = "Scientific object experiment URI")
    private URI experiment;

    @JsonProperty("experiment_name")
    @ApiModelProperty(value = "Scientific object experiment name")
    private String experimentLabel;

    public URI getExperiment() {
        return experiment;
    }

    public void setExperiment(URI experiment) {
        this.experiment = experiment;
    }

    public String getExperimentLabel() {
        return experimentLabel;
    }

    public void setExperimentLabel(String experimentLabel) {
        this.experimentLabel = experimentLabel;
    }

    @Override
    public void toModel(ScientificObjectModel model) {
        super.toModel(model);
    }

    @Override
    public ScientificObjectModel newModelInstance() {
        return new ScientificObjectModel();
    }

    static ScientificObjectDetailByExperimentsDTO getDTOFromModel(ScientificObjectModel model, ExperimentModel experiment, GeospatialModel geometryByURI, MoveModel lastMove) throws JsonProcessingException {
        ScientificObjectDetailByExperimentsDTO dto = new ScientificObjectDetailByExperimentsDTO();
        dto.fromModel(model, lastMove);
        if (geometryByURI != null) {
            dto.setGeometry(geometryToGeoJson(geometryByURI.getGeometry()));
        }

        if (experiment != null) {
            dto.setExperiment(experiment.getUri());
            dto.setExperimentLabel(experiment.getName());
        }
        return dto;
    }
}
