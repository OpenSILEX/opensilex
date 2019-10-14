//******************************************************************************
//                                Contact.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 9 juil. 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

/**
 *
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class Contact {
    private String uri;
    private String email;
    private String firstname;
    private String lastname;
    
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
    
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }
}
