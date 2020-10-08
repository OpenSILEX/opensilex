//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api.quality;

import io.swagger.annotations.ApiModelProperty;

import java.net.URI;

import javax.validation.constraints.NotNull;

public class QualityUpdateDTO extends QualityCreationDTO {

    @NotNull
    @Override
    @ApiModelProperty(required = true)
    public URI getUri() {
        return super.getUri();
    }

}
