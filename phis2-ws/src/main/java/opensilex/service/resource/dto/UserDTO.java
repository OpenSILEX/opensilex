//******************************************************************************
//                               UserDTO.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: April 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import javax.validation.constraints.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.model.User;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.model.Group;

/**
 * User DTO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class UserDTO extends AbstractVerifiedClass {

    final static Logger LOGGER = LoggerFactory.getLogger(UserDTO.class);
    
    private String email;
    private String password;
    private String firstName;
    private String familyName;
    private String address;
    private String phone;
    private String affiliation;
    private String orcid;
    private String admin;
    private ArrayList<String> groupsUris = new ArrayList<>();
    
    @Override
    public User createObjectFromDTO() {
        User user = new User(email);
        user.setPassword(password);
        user.setFirstName(firstName);
        user.setFamilyName(familyName);
        user.setAddress(address);
        user.setPhone(phone);
        user.setAffiliation(affiliation);
        user.setOrcid(orcid);
        user.setAdmin(admin);
        
        if (groupsUris != null) {
            for (String groupURI : groupsUris) {
                Group group = new Group(groupURI);
                user.addGroup(group);
            }
        }
        
        return user;
    }
    
    @Email
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_USER_EMAIL)
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_USER_PASSWORD)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_USER_FIRST_NAME)
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_USER_FAMILY_NAME)
    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_USER_ADDRESS)
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_USER_PHONE)
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_USER_AFFILIATION)
    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    @URL
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_USER_ORCID)
    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_USER_ADMIN)
    public String getAdmin() {
        return admin;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
    }
    
    @URL
    public ArrayList<String> getGroupsUris() {
        return groupsUris;
    }

    public void setGroupsUris(ArrayList<String> groupsUris) {
        this.groupsUris = groupsUris;
    }
}
