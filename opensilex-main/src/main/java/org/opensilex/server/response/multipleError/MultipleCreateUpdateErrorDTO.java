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

@ApiModel
public class MultipleCreateUpdateErrorDTO extends MultipleErrorDTO {

    /**
     * in the context of a creation/update service, this boolean is true if the object is in update mode (i.e. it's uri already exists)
     */
    @ApiModelProperty(value = "uri", example = "http://exemple")
    public final boolean isUpdate;


    public MultipleCreateUpdateErrorDTO(String title, List<String> errors, boolean isUpdate) {
        super(title, errors);
        this.isUpdate = isUpdate;
    }
}
