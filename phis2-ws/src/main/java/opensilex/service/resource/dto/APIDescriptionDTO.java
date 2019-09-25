//******************************************************************************
//                       APIDescriptionDTO.java
// SILEX-PHIS
// Copyright © INRA
// Creation date: 25 Septembre 2019
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto;

import io.swagger.annotations.ApiModelProperty;
import opensilex.service.PropertiesFileManager;
import opensilex.service.documentation.DocumentationAnnotation;

/**
 * API description model
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class APIDescriptionDTO {
    
    /**
     * Version of the API
     * @example "v.3.3.0" using semantic versionning
     */
    public String version;
    /**
     * Major version of the API
     * @example 3, the first version number 
     */
    public Integer majorVersion;
    /**
     * Name of the API
     * @example OpenSILEX API
     */
    public String name;
    /**
     * The build identifier
     * @example 
     */
    public String build;

    public APIDescriptionDTO() {
        String propertyVersion = PropertiesFileManager.getConfigFileProperty("pom", "version");
        String apiName = PropertiesFileManager.getConfigFileProperty("pom", "apiName");
        this.version = propertyVersion;
        String firstVersionNumber = Character.toString(propertyVersion.replace("v", "").charAt(0));
        this.majorVersion = Integer.parseInt(firstVersionNumber);
        this.name = apiName;
    }
}
