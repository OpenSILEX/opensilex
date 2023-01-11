//******************************************************************************
//                          UserCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.person.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.server.rest.validation.Required;

import javax.mail.internet.InternetAddress;
import java.net.URI;
import java.util.Objects;

/**
 * <pre>
 * DTO repensenting JSON for person creation.
 *
 * JSON representation:
 * {
 *      uri: ... optional custom uri, auto-generated if missing
 *      firstName: ... user first name
 *      lastName: ... user last name
 * }
 * </pre>
 *
 * @author Yvan Roux
 */
@ApiModel
@JsonPropertyOrder({"uri", "first_name", "last_name", "email"})
public class PersonDTO {


    protected URI uri;

    @JsonProperty("first_name")
    @Required(message = "first name is required to create a person")
    protected String firstName;

    @JsonProperty("last_name")
    @Required(message = "last name is required to create a person")
    protected String lastName;

    protected String email;

    @ApiModelProperty(value = "Person URI", example = "http://opensilex.dev/person#harold.haddock.mistea")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @ApiModelProperty(value = "Person first name", example = "Harold")
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @ApiModelProperty(value = "Person last name", example = "Haddock")
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @ApiModelProperty(value = "email", example = "harold-h@inrae.fr")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * convert a PersonModel into a PersonDTO.
     *
     * @param personModel to convert
     * @return a PersonDTO
     */
    public static PersonDTO fromModel(PersonModel personModel) {
        PersonDTO personDTO = new PersonDTO();

        personDTO.setUri(personModel.getUri());
        personDTO.setFirstName(personModel.getFirstName());
        personDTO.setLastName(personModel.getLastName());
        InternetAddress email = personModel.getEmail();
        if (email != null) {
            personDTO.setEmail(email.toString());
        }

        return personDTO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersonDTO personDTO = (PersonDTO) o;
        return Objects.equals(uri, personDTO.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }
}
