//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api;


import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import java.net.URI;

public class VariableUpdateDTO extends VariableCreationDTO {

    @Override
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    public URI getUri() {
        return uri;
    }

}
