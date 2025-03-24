/*
 * *****************************************************************************
 *                         MultipleErrorDTO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2025.
 * Last Modification: 11/03/2025 11:24
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.server.response.multipleError;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 * DTO to capture multiple errors about a single object
 */
@ApiModel
public class MultipleErrorDTO {

    @ApiModelProperty(value = "uri", example = "http://exemple")
    public final String uri;

    @ApiModelProperty(value = "errors")
    public final List<String> errors;


    public MultipleErrorDTO(String uri, List<String> errors) {
        this.uri = uri;
        this.errors = errors;
    }
}
