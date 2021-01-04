//******************************************************************************
//                          AnnotationUpdateDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.annotation.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.net.URI;

/**
 * @author Renaud COLIN
 */
@ApiModel
public class AnnotationUpdateDTO extends AnnotationCreationDTO {

    @NotNull
    @Override
    @ApiModelProperty(required = true)
    public URI getUri() {
        return super.getUri();
    }
}
