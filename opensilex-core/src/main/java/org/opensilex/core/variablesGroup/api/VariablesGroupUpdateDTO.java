//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: hamza.ikiou@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variablesGroup.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import javax.validation.constraints.NotNull;

/**
 *
 * @author Hamza Ikiou
 */
public class VariablesGroupUpdateDTO extends VariablesGroupCreationDTO{
    
    @Override
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public URI getUri() {
        return uri;
    }
}
