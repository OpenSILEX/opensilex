//******************************************************************************
//                        ApiContactInfoDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.system.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import java.net.URL;

/**
 * Class that represents informations about system contact
 * @author Arnaud Charleroy
 */
@JsonPropertyOrder({"name", "email", "homepage"})
public class ApiContactInfoDTO {

    private String name;
   
    private String email;
     
    private URL homepage;

    public ApiContactInfoDTO() {
    }

    public ApiContactInfoDTO(URL url) {
        this.homepage = url;
    }

    public ApiContactInfoDTO(String name, String email, URL url) {
        this.name = name;
        this.email = email;
        this.homepage = url;
    }
    
    @ApiModelProperty(value = "Opensilex Team", example = "Opensilex Team")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    @ApiModelProperty(value = "opensilex@gmail.com", example = "opensilex@gmail.com")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public URL getHomepage() {
        return homepage;
    }

    public void setHomepage(URL homepage) {
        this.homepage = homepage;
    }
}
