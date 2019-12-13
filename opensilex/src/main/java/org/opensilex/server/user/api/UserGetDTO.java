/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.user.api;

import java.net.URI;
import org.opensilex.server.user.dal.UserModel;

/**
 *
 * @author vince
 */
public class UserGetDTO {

    private URI uri;
    
    private String email;

    private String firstName;

    private String lastName;

    private boolean admin;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }
    
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
