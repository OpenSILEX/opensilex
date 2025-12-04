//******************************************************************************
//                          GeneticResourceGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.geneticResource.api;

import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import javax.validation.constraints.NotNull;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author Alice Boizet
 */
public class GeneticResourceUpdateDTO extends GeneticResourceCreationDTO {
    
    @NotNull
    @ValidURI
    @ApiModelProperty(value = "GeneticResource URI", example = "http://opensilex.dev/opensilex/id/plantMaterialLot#SL_001")
    @Override
    public URI getUri() {
        return uri;
    }
    
}
