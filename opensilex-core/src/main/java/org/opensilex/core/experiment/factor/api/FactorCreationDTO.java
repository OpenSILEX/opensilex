/*
 * ******************************************************************************
 *                                     FactorCreationDTO.java
 *  OpenSILEX
 *  Copyright © INRA 2019
 *  Creation date:  17 December, 2019
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.experiment.factor.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.server.rest.validation.FilteredName;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author Arnaud Charleroy
 */

@JsonPropertyOrder({
        "uri", "name", "category", "description", "levels",
        SKOSReferencesDTO.EXACT_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.CLOSE_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.BROAD_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.NARROW_MATCH_JSON_PROPERTY
})
public class FactorCreationDTO extends SKOSReferencesDTO {

    protected URI uri;

    private String name;

    private URI category;

    private String description;

    private URI experiment;

    @Valid
    @NotNull
    @JsonProperty("levels")
    private List<FactorLevelCreationDTO> factorLevels;

    @ValidURI
    @ApiModelProperty(example = "http://opensilex.dev/set/factors#irrigation")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @Required
    @FilteredName
    @ApiModelProperty(required = true, example = "Irrigation")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(example = "http://aims.fao.org/aos/agrovoc/c_5b384c25")
    public URI getCategory() {
        return category;
    }

    public void setCategory(URI category) {
        this.category = category;
    }

    public URI getExperiment() {
        return experiment;
    }

    public void setExperiment(URI experiment) {
        this.experiment = experiment;
    }

    @ApiModelProperty(example = "Experimental factor about water exposure")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<FactorLevelCreationDTO> getFactorLevels() {
        return factorLevels;
    }

    public void setFactorLevels(List<FactorLevelCreationDTO> factorsLevels) {
        this.factorLevels = factorsLevels;
    }

    public FactorModel newModel() {
        FactorModel model = new FactorModel();
        model.setUri(getUri());
        model.setName(getName());
        if(getCategory() != null){
            model.setCategory(getCategory());
        }
        model.setDescription(getDescription());
        this.setSkosReferencesToModel(model);
        List<ExperimentModel> associatedExperiment = new ArrayList<>();
        ExperimentModel experimentModel = new ExperimentModel();
        experimentModel.setUri(getExperiment());
        associatedExperiment.add(experimentModel);
        model.setExperiment(experimentModel);
        model.setAssociatedExperiments(associatedExperiment);

        return model;
    }
}
