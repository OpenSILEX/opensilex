//**********************************************************************************************
//                                       GroupDTO.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: April 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  April, 2017
// Subject: A class which contains methods to automatically check the attributes
//          of a class, from rules defined by user.
//          Contains the list of the elements which might be send by the Client
//          to save the database
//***********************************************************************************************
package phis2ws.service.resources.dto.group;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import javax.validation.constraints.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.model.User;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.resources.validation.interfaces.GroupLevel;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.view.model.phis.Group;

public class GroupDTO extends AbstractVerifiedClass {
    
    final static Logger LOGGER = LoggerFactory.getLogger(GroupDTO.class);
    
    private String uri;
    private String name;
    private String level;
    private String description;
    
    private ArrayList<String> usersEmails = new ArrayList<>();

 
    @Override
    public Group createObjectFromDTO() {
        Group group = new Group(uri);
        group.setName(name);
        group.setLevel(level);
        group.setDescription(description);
        
        if (usersEmails != null) {
            for (String userEmail : usersEmails) {
                User u = new User(userEmail);
                group.addUser(u);
            }
        }
        
        return group;
    }

    @URL
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_GROUP_URI)
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
    
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_GROUP_NAME)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    @GroupLevel
    @Required
    @ApiModelProperty(example = "Admin")
    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }
    
    @Required
    @ApiModelProperty(example = "description of the gamma group")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public ArrayList<@Email String> getUsersEmails() {
        return usersEmails;
    }

    public void setUsersEmails(ArrayList<String> usersEmails) {
        this.usersEmails = usersEmails;
    }
}
