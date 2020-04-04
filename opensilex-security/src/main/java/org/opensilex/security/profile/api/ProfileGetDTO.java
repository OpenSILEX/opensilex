//******************************************************************************
//                          UserGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.profile.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.util.List;
import org.opensilex.security.profile.dal.ProfileModel;

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
 * @see org.opensilex.rest.profile.api.ProfileAPI#searchProfiles(java.lang.String, java.util.List, int, int) 
 * @author Vincent Migot
 */
@ApiModel
public class ProfileGetDTO {

    /**
     * User URI
     */
    protected URI uri;

    /**
     * User email
     */
    protected String name;

    /**
     * User first name
     */
    protected List<String> credentials;

    @ApiModelProperty(value = "User URI", example = "http://opensilex.dev/users#agent.Admin_OpenSilex")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @ApiModelProperty(value = "Profile name", example = "profile1")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(value = "Profile credentials")
    public List<String> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<String> credentials) {
        this.credentials = credentials;
    }

    /**
     * Convert User Model into User DTO
     *
     * @param model User Model to convert
     * @return Corresponding user DTO
     */
    public static ProfileGetDTO fromModel(ProfileModel model) {
        ProfileGetDTO dto = new ProfileGetDTO();

        dto.setUri(model.getUri());
        dto.setName(model.getName());
        dto.setCredentials(model.getCredentials());

        return dto;
    }

}
