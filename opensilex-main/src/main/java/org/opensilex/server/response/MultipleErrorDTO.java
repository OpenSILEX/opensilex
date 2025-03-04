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

/**
 * Error DTO class.
 * <pre>
 * This class define an error DTO used by {@code org.opensilex.server.response.ErrorResponse}
 * It's defined by a title and a message and eventually a source exception.
 * A translation key (and values) can also be provided if the error should be displayed to the user.
 *
 * ONLY IN DEBUG MODE:
 * If error result is constructed with an exception result will contains two version of the stack trace arrays
 * - "stack" contains exception stack trace array filtered by packages org.opensilex.*
 * - "fullstack" contains complete exception stack trace array
 * </pre>
 *
 * @see ErrorResponse
 * @author Vincent Migot
 */
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
    @ApiModelProperty(value = "every errors")
    public Map<String, String> errors;


    public MultipleErrorDTO(String title, Map<String, String> errors) {
        this.title = title;
        this.errors = errors;
    }
}
