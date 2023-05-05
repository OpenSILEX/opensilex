//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: maximilian.hart@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.germplasmGroup.api;

import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.net.URI;

/**
 *
 * @author Maximilian HART
 */
public class GermplasmGroupUpdateDTO extends GermplasmGroupCreationDTO {
    
    @Override
    @NotNull
    @ApiModelProperty(required = true)
    public URI getUri() {
        return uri;
    }
}
