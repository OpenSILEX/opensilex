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

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import org.opensilex.core.experiment.api.ExperimentAPI;
import org.opensilex.core.experiment.factor.dal.FactorModel;

/**
 *
 * @author Arnaud Charleroy
 */
public class FactorGetDTO {
    
    @JsonPropertyOrder({ "uri", "name", "category", "description" })
        
    @ApiModelProperty(example = "http://opensilex.dev/set/factors#irrigation")
    private URI uri;
    
    @ApiModelProperty(example = "Irrigation")
    private String name;
    
    @ApiModelProperty(example = "waterManagement")
    private URI category;
    
    @ApiModelProperty(example = "Experimental factor about water management")
    private String description;

    @ApiModelProperty(example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI)
    private URI experiment;
    
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
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
    
    public static FactorGetDTO fromModel(FactorModel model) {
        FactorGetDTO dto = new FactorGetDTO();
        dto.setUri(model.getUri());        
        dto.setName(model.getName());
        dto.setDescription(model.getDescription());
        if(model.getCategory() != null){
            dto.setCategory(model.getCategory().getUri());
        }
        dto.setExperiment(model.getExperiment().getUri());
        return dto;
    }
}
