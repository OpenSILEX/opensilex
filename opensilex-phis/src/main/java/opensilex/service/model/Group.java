//******************************************************************************
//                                Group.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: April 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import java.util.ArrayList;

/**
 * Group model.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class Group {
    private String uri;
    private String name;
    private String level;
    private String description;
    private ArrayList<User> users = new ArrayList<>();
    
    public Group() {
    }
    
    public Group(String uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
    
    public boolean equals(Group group) {
        return this.uri.equals(group.uri);
    }
    
    public ArrayList<User> getUsers() {
        return this.users;
    }
    
    public void addUser(User u) {
        this.users.add(u);
    }
    
    public void setUserList(ArrayList<User> users) {
        this.users = users;
    }
}
