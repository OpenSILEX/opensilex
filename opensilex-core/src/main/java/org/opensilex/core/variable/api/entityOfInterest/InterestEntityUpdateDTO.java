//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: hamza.ikiou@inrae.fr, renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variable.api.entityOfInterest;

import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import javax.validation.constraints.NotNull;

/**
 * @author Hamza IKIOU
 */
public class InterestEntityUpdateDTO extends InterestEntityCreationDTO {
    
    @NotNull
    @Override
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "http://opensilex.dev/set/variables/entity_of_interest/Plot")
    public URI getUri() {
        return super.getUri();
    }
}
