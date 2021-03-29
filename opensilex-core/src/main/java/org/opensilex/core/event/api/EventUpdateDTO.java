//******************************************************************************
//                          EventUpdateDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.event.api;

import io.swagger.annotations.ApiModel;

import javax.validation.constraints.NotNull;
import java.net.URI;

/**
 * @author Renaud COLIN
 */
@ApiModel
public class EventUpdateDTO extends EventCreationDTO{

    @Override
    @NotNull
    public URI getUri() {
        return super.getUri();
    }
}
