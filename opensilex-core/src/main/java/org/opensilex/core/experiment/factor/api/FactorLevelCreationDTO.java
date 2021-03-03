/*
 * ******************************************************************************
 *                                     FactorLevelCreationDTO.java
 *  OpenSILEX
 *  Copyright © INRAE 2020
 *  Creation date:  11 March, 2020
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.experiment.factor.api;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;

/**
 * 
 * @author Arnaud Charleroy
 */
public class FactorLevelCreationDTO extends FactorLevelGetDTO{

    
    @ApiModelProperty(example = "WW")
    @Override
    @NotNull
    public String getName() {
        return name;
    }
    
    public FactorLevelModel newModel() {
        FactorLevelModel model = new FactorLevelModel();
        model.setUri(getUri());
        model.setName(getName());
        model.setDescription(getDescription());
        return model;
    }
}
