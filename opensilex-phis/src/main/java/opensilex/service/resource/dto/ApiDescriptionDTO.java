//******************************************************************************
//                       ApiDescriptionDTO.java
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
public class ApiDescriptionDTO {
    
    /**
     * Property file used to retreive system informations
     */
    final static String PROPERTY_FILE_NAME = "service";

    
    /**
     * Version of the API
     * @example "3.3.0" using semantic versionning
     */  
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_API_BUILD_VERSION)
    public String buildVersion;
    
    /**
     * Major version of the API
     * @example 3, the first version number 
     */
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_API_MAJOR_VERSION)
    public Integer majorVersion;
    
    /**
     * Name of the API
     * @example OpenSILEX API
     */
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_API_NAME)
    public String name;

    /**
    * APIDescriptionDTO api description DTO
    * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
    */
    public ApiDescriptionDTO() {
        String propertyVersion = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILE_NAME, "wsVersion");
        String apiName = PropertiesFileManager.getConfigFileProperty(PROPERTY_FILE_NAME, "wsApiName");
        this.buildVersion = propertyVersion;
        String firstVersionNumber = Character.toString(propertyVersion.charAt(0));
        this.majorVersion = Integer.parseInt(firstVersionNumber);
        this.name = apiName;
    }
}
