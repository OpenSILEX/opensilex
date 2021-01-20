//********Germplasm**********************************************************************
//                          AreaUpdateDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.area.api;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.server.rest.validation.ValidURI;

import javax.validation.constraints.NotNull;
import java.net.URI;

/**
 * @author Jean Philippe VERT
 */
public class AreaUpdateDTO extends AreaCreationDTO {
    @NotNull
    @ValidURI
    @ApiModelProperty(value = "Area URI", example = "http://opensilex/set/area/Z_001")
    @Override
    public URI getUri() {
        return uri;
    }
}