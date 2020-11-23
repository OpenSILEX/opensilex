//******************************************************************************
//                          DataFileUpdateDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import java.net.URI;
import javax.validation.constraints.NotNull;

/**
 *
 * @author Alice Boizet
 */
public class DataFileUpdateDTO extends DataFilePathCreationDTO {
    
    @NotNull
    @Override
    public URI getUri() {
        return uri;
    }
    
}
