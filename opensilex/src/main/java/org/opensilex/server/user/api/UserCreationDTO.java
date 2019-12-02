//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.user.api;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.Email;
import org.opensilex.server.validation.Required;


/**
 *
 * @author vincent
 */
public class UserCreationDTO {

    private String email;

    private String firstName;

    private String lastName;

    private String password;

    @Email
    @Required
    @ApiModelProperty(example = "john.doe@example.com")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Required
    @ApiModelProperty(example = "John")
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Required
    @ApiModelProperty(example = "Doe")
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @ApiModelProperty(example = "azerty")
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
