/*
 * *****************************************************************************
 *                         MultipleErrorDTO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2025.
 * Last Modification: 04/03/2025 14:05
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.server.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApiModel
public class MultipleErrorDTO {

    /**
     * Title of the error.
     */
    @ApiModelProperty(value = "Title of the error", example = "Error")
    public final String title;

    /**
     * every errors
     */
    @ApiModelProperty(value = "every errors", example = "http://example.com : {error1, error 2}")
    public Map<String, List<String>> errors;


    public MultipleErrorDTO(String title, Map<String, List<String>> errors) {
        this.title = title;
        this.errors = errors;
    }
}
