//******************************************************************************
//                               DataLogAccessUserDTO.java 
// SILEX-PHIS
// Copyright © INRAE 2020
// Creation date: February 2020
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package opensilex.service.resource.dto.data;

import io.swagger.annotations.ApiModelProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.model.User;

/**
 * DataLogAccessUser DTO.
* @author Arnaud Charleroy
 */
public class DataLogAccessUserDTO extends AbstractVerifiedClass {

    final static Logger LOGGER = LoggerFactory.getLogger(DataLogAccessUserDTO.class);
    
    private String email;
    private String firstName;
    private String familyName;
    private String uri;
    
    @Override
    public User createObjectFromDTO() {
        User user = new User(email);
        user.setFirstName(firstName);
        user.setFamilyName(familyName);
        
        return user;
    }
     
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_USER_EMAIL)
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_USER_FIRST_NAME)
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_USER_LASTNAME)
    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_USER_URI)
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}