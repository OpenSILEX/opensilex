/*
 * *****************************************************************************
 *                         MultipleErrorListDTO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2025.
 * Last Modification: 01/07/2025 10:57
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.server.response.multipleError;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 * DTO to capture multiple errors about multiple objects. Each object can have multiple errors, see {@link MultipleErrorDTO}.
 */
@ApiModel
public class MultipleErrorListDTO {

    /**
     * Title of the error.
     */
    @ApiModelProperty(value = "Title of the error", example = "Error")
    public final String title;

    /**
     * every errors
     */
    @ApiModelProperty(value = "every errors", example = "{ [ {http://exemple1, [error1, error2]}, {http://exemple2, [error3, error4]} ] }")
    public List<MultipleErrorDTO> errors;


    public MultipleErrorListDTO(String title, List<MultipleErrorDTO> errors) {
        this.title = title;
        this.errors = errors;
    }
}
