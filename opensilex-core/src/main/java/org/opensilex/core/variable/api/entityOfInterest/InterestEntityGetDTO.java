//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: hamza.ikiou@inrae.fr, renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variable.api.entityOfInterest;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.variable.api.BaseVariableGetDTO;
import org.opensilex.core.variable.dal.InterestEntityModel;

import java.net.URI;

/**
 * @author Hamza IKIOU
 */
public class InterestEntityGetDTO extends BaseVariableGetDTO<InterestEntityModel> {
    
    public InterestEntityGetDTO(InterestEntityModel model){
        super(model);
    }

    public InterestEntityGetDTO(){
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
}
