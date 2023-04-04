//******************************************************************************
//                          UserCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.user.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import org.opensilex.server.rest.validation.Required;

/**
 * <pre>
 * DTO repensenting JSON for user creation.
 *
 * JSON representation:
 * {
 *      uri: ... optional custom uri, auto-generated if missing
 *      Person_uri : the URI of the person who hold this account (user = account)
 *      email: ... user email
 *      language: ... user language
 *      password: ... user password
 *      admin: ... flag to define if user is an admin or not
 * }
 * </pre>
 *
 * @author Yvan Roux
 */
@ApiModel
@JsonPropertyOrder({"uri", "account_holder", "email", "language", "password", "admin"})
public class UserCreationWithExistantPersonDTO extends UserDTO{

    protected String password;
    /**
     * URI of the Person who hold the account
     */
    @JsonProperty("account_holder")
    protected URI holderOfTheAccount;

    @ApiModelProperty(required = true, value = "URI of the Person who will hold this account", example = "http://opensilex.dev/person#Jean.Michel.mistea")
    public URI getHolderOfTheAccount(){
        return holderOfTheAccount;
    }
    public void setHolderOfTheAccount(URI uri){ this.holderOfTheAccount = uri; }

    @Required
    @ApiModelProperty(value = "User password", example = "azerty", required = true)
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

}
