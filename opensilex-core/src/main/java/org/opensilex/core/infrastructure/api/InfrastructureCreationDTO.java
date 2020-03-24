/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.infrastructure.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.opensilex.core.infrastructure.dal.InfrastructureModel;
import org.opensilex.rest.user.dal.UserModel;

/**
 *
 * @author vince
 */
class InfrastructureCreationDTO {
    
    private URI uri;
    
    private URI type;

    private String name;
    
    private URI parent;
    
    private List<URI> users;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URI getParent() {
        return parent;
    }

    public void setParent(URI parent) {
        this.parent = parent;
    }

    public List<URI> getUsers() {
        return users;
    }

    public void setUsers(List<URI> users) {
        this.users = users;
    }
    
    public InfrastructureModel newModel() {
        InfrastructureModel model = new InfrastructureModel();
        model.setUri(getUri());
        model.setName(getName());
        if (getType() != null) {
            model.setType(getType());
        }
        if (getParent() != null) {
            InfrastructureModel parentModel = new InfrastructureModel();
            parentModel.setUri(getParent());
            model.setParent(parentModel);
        }
        
        if (getUsers() != null) {
            List<UserModel> users = new ArrayList<>();
            getUsers().forEach(userURI -> {
                UserModel user = new UserModel();
                user.setUri(userURI);
                users.add(user);
            });
            model.setUsers(users);
        }
        
        return model;
    }
}
