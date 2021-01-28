//******************************************************************************
//                          DataFileCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import javax.validation.constraints.NotNull;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author sammy
 */
public class DataUpdateDTO  extends DataCreationDTO {
    
    @NotNull
    @ValidURI
    @Override
    @ApiModelProperty(value = "URI of the data being updated", example = DataAPI.DATA_EXAMPLE_URI) 
    public URI getUri() {
        return uri;
    }

}
