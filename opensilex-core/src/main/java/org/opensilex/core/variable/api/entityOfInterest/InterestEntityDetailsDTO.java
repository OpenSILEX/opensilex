//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: hamza.ikiou@inrae.fr, renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variable.api.entityOfInterest;

import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import org.opensilex.core.variable.api.BaseVariableDetailsDTO;
import org.opensilex.core.variable.dal.InterestEntityModel;

/**
 * @author Hamza IKIOU
 */
public class InterestEntityDetailsDTO extends BaseVariableDetailsDTO<InterestEntityModel> {
    
    public InterestEntityDetailsDTO(InterestEntityModel model){
        super(model);        
    }
    
    public InterestEntityDetailsDTO(){       
    }

    @Override
    @ApiModelProperty(example = "http://opensilex.dev/set/variables/entity_of_interest/Plot")
    public URI getUri() {
        return uri;
    }

    @Override
    @ApiModelProperty(example = "Plot")
    public String getName() {
        return name;
    }

    @Override
    @ApiModelProperty(example = "The entity of interest which characterizes a plot")
    public String getDescription() {
        return description;
    }
}
