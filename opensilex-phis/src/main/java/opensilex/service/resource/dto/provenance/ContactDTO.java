//******************************************************************************
//                                ContactDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 9 juil. 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.provenance;

import io.swagger.annotations.ApiModelProperty;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.model.Contact;

/**
 * DTO of the contacts for the projects.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ContactDTO {
    //URI of the contact.
    private String uri;
    //Firstname of the contact
    private String firstname;
    //Lastname of the contact
    private String lastname;
    //Email of the contact
    private String email;
    
    public ContactDTO(Contact contact) {
        uri = contact.getUri();
        firstname = contact.getFirstname();
        lastname = contact.getLastname();
        email = contact.getEmail();
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_USER_URI)
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_USER_FIRST_NAME)
    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_USER_LASTNAME)
    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_USER_EMAIL)
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
