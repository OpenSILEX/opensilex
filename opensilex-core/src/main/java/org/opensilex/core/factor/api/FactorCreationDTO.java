/*
 * ******************************************************************************
 *                                     FactorCreationDTO.java
 *  OpenSILEX
 *  Copyright © INRA 2019
 *  Creation date:  17 December, 2019
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.factor.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.util.List;
import javax.validation.Valid;
import org.opensilex.core.factor.dal.FactorModel;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author Arnaud Charleroy
 */
public class FactorCreationDTO extends SKOSReferencesDTO {
    @JsonPropertyOrder({"uri", "name", "category", "description", "factor_levels"})

    @ValidURI
    @ApiModelProperty(example = "http://opensilex.dev/set/factors#irrigation")
    private URI uri;

    @Required 
    @ApiModelProperty(example = "Irrigation")
    private String name;
    
    @ApiModelProperty(example = "waterManagement")
    private String category;
    
    @ApiModelProperty(example = "Experimental factor about water management")
    private String description;
    
    @Valid
    @JsonProperty("factor_levels")
    private List<FactorLevelCreationDTO> factorLevels;

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

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
        model.setCategory(getCategory());
        model.setDescription(getDescription());
        this.setSkosReferencesToModel(model);

        return model;
    }
}
