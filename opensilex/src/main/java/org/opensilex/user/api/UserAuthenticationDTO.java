//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.user.api;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.server.validation.Required;

/**
 *
 * @author vincent
 */
public class UserAuthenticationDTO {

    private String identifier;

    private String password;

    @Required
    @ApiModelProperty(example = "john.doe@example.com")
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Required
    @ApiModelProperty(example = "azerty")
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
