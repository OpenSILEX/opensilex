//******************************************************************************
//                          AnnotationUpdateDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.annotation.api;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import java.net.URI;

/**
 * @author Renaud COLIN
 */
@Schema
public class AnnotationUpdateDTO extends AnnotationCreationDTO {

    @NotNull
    @Override
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "http://www.opensilex.org/annotations/12590c87-1c34-426b-a231-beb7acb33415")
    public URI getUri() {
        return super.getUri();
    }
}
