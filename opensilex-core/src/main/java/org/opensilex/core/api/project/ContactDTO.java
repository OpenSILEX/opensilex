/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.api.project;

import java.net.URI;
import org.opensilex.server.security.user.User;

/**
 * DTO of the contacts for the projects
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ContactDTO {
    private URI uri;
    private String firstname;
    private String lastname;

    public ContactDTO(User user) {
        uri = user.getUri();
        firstname = user.getFirstName();
        lastname = user.getLastName();
    }

    public URI getUri() {
        return uri;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }
}
