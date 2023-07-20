//******************************************************************************
//                          UserGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.user.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.person.api.PersonDTO;

import java.net.URI;
import java.util.List;
import java.util.Objects;

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
 * @author Vincent Migot
 */
@ApiModel
@JsonPropertyOrder({"uri", "email", "language",
    "admin", "first_name", "last_name", "linked_person"})
public class UserGetDTO extends UserDTO {

    @JsonProperty("first_name")
    protected String firstName;

    @JsonProperty("last_name")
    protected String lastName;

    /**
     * List of favorites
     */
    protected List<URI> favorites;

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

    @ApiModelProperty(value = "Favorites URI", example = "test")
    public List<URI> getFavorites() {
        return favorites;
    }

    public void setFavorites(List<URI> favorites) {
        this.favorites = favorites;
    }

    /**
     * Convert User Model into User DTO
     *
     * @param model User Model to convert
     * @return Corresponding user DTO
     */
    public static UserGetDTO fromModel(AccountModel model) {
        UserGetDTO dto = new UserGetDTO();

        dto.setUri(model.getUri());
        dto.setAdmin(model.isAdmin());
        dto.setEmail(model.getEmail().toString());
        dto.setLanguage(model.getLanguage());
        dto.setEnable(model.getIsEnabled());
        if (Objects.nonNull(model.getLinkedPerson()) ){
            dto.setFirstName(model.getLinkedPerson().getFirstName());
            dto.setLastName(model.getLinkedPerson().getLastName());
            dto.setLinkedPerson(model.getLinkedPerson().getUri());
        }

        return dto;
    }

    public PersonDTO createCorrespondingPersonDTO() {
        PersonDTO personDTO = new PersonDTO();
        personDTO.setUri(linkedPerson);
        personDTO.setFirstName(firstName);
        personDTO.setLastName(lastName);
        personDTO.setEmail(email);
        personDTO.setAccount(uri);

        return personDTO;
    }
}
