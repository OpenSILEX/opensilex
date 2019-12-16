//******************************************************************************
//                          UserCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.security.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.Email;
import org.opensilex.server.validation.Required;

/**
 * <pre>
 * DTO repensenting JSON for user creation.
 *
 * JSON representation:
 * {
 *      email: ... user email
 *      firstName: ... user first name
 *      lastName: ... user last name
 *      admin: ... flag to define if user is an admin or not
 *      password: ... user password
 * }
 * </pre>
 *
 * @see
 * org.opensilex.server.security.api.UserAPI#create(org.opensilex.server.security.api.UserCreationDTO,
 * javax.ws.rs.core.SecurityContext)
 * @author Vincent Migot
 */
@ApiModel
public class UserCreationDTO {

    /**
     * User email
     */
    private String email;

    /**
     * User first name
     */
    private String firstName;

    /**
     * User last name
     */
    private String lastName;

    /**
     * Determine if user is admin or not
     */
    private boolean admin;

    private String password;

    @Email
    @Required
    @ApiModelProperty(value = "User email", example = "jean.michel@example.com")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Required
    @ApiModelProperty(value = "User first name", example = "Jean")
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Required
    @ApiModelProperty(value = "User last name", example = "Michel")
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Required
    @ApiModelProperty(value = "User password", example = "azerty")
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Required
    @ApiModelProperty(value = "User admin flag", example = "false")
    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }
}
