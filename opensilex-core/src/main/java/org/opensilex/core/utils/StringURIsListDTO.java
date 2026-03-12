/*
 * *****************************************************************************
 *                         StringURIsListDTO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 03/02/2026 08:59
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.core.utils;

import io.swagger.annotations.ApiModelProperty;

import java.util.List;

public class StringURIsListDTO {

    @ApiModelProperty(value = "URIs", example = "[http://opensilex.dev/opensilex/id/my/uri, http://opensilex.dev/opensilex/id/another_uri]")
    protected List<String> uris;

    public List<String> getUris() {
        return uris;
    }

    public void setUris(List<String> uris) {
        this.uris = uris;
    }
    
}
