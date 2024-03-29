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
import org.opensilex.security.account.dal.AccountModel;
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
 *      firstName: ... required first name
 *      lastName: ... required last name
 *      email: ... optional email
 *      account: ... uri of the account own by this person
 * }
 * </pre>
 *
 * @author Yvan Roux
 */
@ApiModel
@JsonPropertyOrder({"uri", "first_name", "last_name", "email", "affiliation", "phone_number", "orcid", "account"})
public class PersonDTO {

    @JsonProperty("uri")
    protected URI uri;

    @JsonProperty("first_name")
    @Required(message = "first name is required to create a person")
    protected String firstName;

    @JsonProperty("last_name")
    @Required(message = "last name is required to create a person")
    protected String lastName;

    @JsonProperty("email")
    protected String email;

    @JsonProperty("affiliation")
    protected String affiliation;

    @JsonProperty("phone_number")
    protected String phoneNumber;

    @JsonProperty("orcid")
    protected URI orcid;


    @JsonProperty("account")
    protected URI account;

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

    @ApiModelProperty(value = "affiliation", example = "MISTEA")
    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    @ApiModelProperty(value = "phone number", example = "+33-1-42-75-90-00")
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @ApiModelProperty(value = "orcid", example = "https://orcid.org/0000-0003-4189-7793")
    public URI getOrcid() { return orcid; }

    public void setOrcid(URI orcid) { this.orcid = orcid; }

    @ApiModelProperty(value = "Uri of the account if this person has one", example = "http://opensilex.dev/users#jean.michel.inrae")
    public URI getAccount() {
        return account;
    }

    public void setAccount(URI account) {
        this.account = account;
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
        personDTO.setAffiliation(personModel.getAffiliation());
        personDTO.setOrcid(personModel.getOrcid());
        InternetAddress email = personModel.getEmail();
        if ( Objects.nonNull(personModel.getPhoneNumber())){
            personDTO.setPhoneNumber(personModel.getPhoneNumber().getSchemeSpecificPart());
        }
        if (email != null) {
            personDTO.setEmail(email.toString());
        }
        AccountModel accountModel = personModel.getAccount();
        if (accountModel != null) {
            personDTO.setAccount(accountModel.getUri());
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
