//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: hamza.ikiou@inrae.fr, renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variable.api.entityOfInterest;

import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import javax.validation.constraints.NotNull;

/**
 * @author Hamza IKIOU
 */
public class InterestEntityUpdateDTO extends InterestEntityCreationDTO {
    
    @NotNull
    @Override
    @ApiModelProperty(required = true, example = "http://opensilex.dev/set/variables/entity_of_interest/Plot")
    public URI getUri() {
        return super.getUri();
    }
}
