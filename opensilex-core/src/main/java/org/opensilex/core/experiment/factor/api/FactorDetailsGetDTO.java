/*
 * ******************************************************************************
 *                                     FactorGetDTO.java
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
import java.util.LinkedList;
import java.util.List;
import static org.opensilex.core.experiment.api.ExperimentAPI.EXPERIMENT_EXAMPLE_URI;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.ontology.SKOSReferencesDTO;

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
public class FactorDetailsGetDTO extends SKOSReferencesDTO {

    @ApiModelProperty(example = "http://opensilex.dev/set/factors#irrigation")
    private URI uri;
    
    @ApiModelProperty(example = "Irrigation")
    private String name;
    
    @ApiModelProperty(example = "waterManagement")
    private URI category;
    
    @ApiModelProperty(example = EXPERIMENT_EXAMPLE_URI)
    private URI experiment;

    
    @ApiModelProperty(example = "Experimental factor about water management")
    private String description;

    @JsonProperty("levels")
    List<FactorLevelGetDTO> factorLevels = new LinkedList<>();

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URI getCategory() {
        return category;
    }

    public void setCategory(URI category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<FactorLevelGetDTO> getFactorLevels() {
        return factorLevels;
    }

    public void setFactorLevels(List<FactorLevelGetDTO> factorLevels) {
        this.factorLevels = factorLevels;
    }

    public URI getExperiment() {
        return experiment;
    }

    public void setExperiment(URI experiment) {
        this.experiment = experiment;
    }
    
    

    public static FactorDetailsGetDTO fromModel(FactorModel model) {
        FactorDetailsGetDTO dto = new FactorDetailsGetDTO();
        dto.setUri(model.getUri());
        dto.setName(model.getName());
        if(model.getCategory() != null){
            dto.setCategory(model.getCategory());
        }
        dto.setDescription(model.getDescription());
        dto.setExperiment(model.getExperiment().getUri());
        List<FactorLevelGetDTO> factorLevels = new ArrayList<>();
        model.getFactorLevels().forEach(factorLevelModel -> {
            FactorLevelGetDTO newFactorLevelDTO = FactorLevelGetDTO.fromModel(factorLevelModel);
            factorLevels.add(newFactorLevelDTO);
        });
        dto.setFactorLevels(factorLevels);
        dto.setSkosReferencesFromModel(model);
        return dto;
    }
}
