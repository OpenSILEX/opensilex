//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api.characteristic;

import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;

import javax.validation.constraints.NotNull;

public class CharacteristicUpdateDTO extends CharacteristicCreationDTO {

    @NotNull
    @Override
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "http://opensilex.dev/set/variables/characteristic/Height")
    public URI getUri() {
        return super.getUri();
    }

}
