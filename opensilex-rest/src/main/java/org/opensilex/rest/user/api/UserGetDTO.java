//******************************************************************************
//                          UserGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.rest.user.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import org.opensilex.rest.user.dal.UserModel;

/**
 * <pre>
 * DTO repensenting JSON for searching users or getting them by uri.
 *
 * JSON representation:
 * {
 *      uri: ... user URI
 *      email: ... user email,
 *      firstName: ... user first name
 *      lastName: ... user last name
 *      admin: ... flag to define if user is an admin or not
 * }
 * </pre>
 *
 * @see org.opensilex.server.security.api.UserAPI#get(java.net.URI)
 * @see org.opensilex.server.security.api.UserAPI#search(java.lang.String,
 * java.util.List, int, int)
 * @author Vincent Migot
 */
@ApiModel
public class UserGetDTO {

    /**
     * User URI
     */
    protected URI uri;

    /**
     * User email
     */
    protected String email;

    /**
     * User first name
     */
    protected String firstName;

    /**
     * User last name
     */
    protected String lastName;

    /**
     * Determine if user is admin or not
     */
    protected boolean admin;

    @ApiModelProperty(value = "User URI", example = "http://opensilex.dev/users#Admin.OpenSilex")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @ApiModelProperty(value = "User email", example = "jean.michel@example.com")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @ApiModelProperty(value = "User first name", example = "Jean")
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @ApiModelProperty(value = "User last name", example = "Michel")
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @ApiModelProperty(value = "User admin flag", example = "false")
    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    /**
     * Convert User Model into User DTO
     *
     * @param model User Model to convert
     * @return Corresponding user DTO
     */
    public static UserGetDTO fromModel(UserModel model) {
        UserGetDTO dto = new UserGetDTO();

        dto.setUri(model.getUri());
        dto.setAdmin(model.isAdmin());
        dto.setEmail(model.getEmail().toString());
        dto.setFirstName(model.getFirstName());
        dto.setLastName(model.getLastName());

        return dto;
    }

}
