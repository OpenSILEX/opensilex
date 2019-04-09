//******************************************************************************
//                                  LayerDTO.java
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: August 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto;

import io.swagger.annotations.ApiModelProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.validation.interfaces.URL;

/**
 * Layer DTO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class LayerDTO extends AbstractVerifiedClass {
    
    /**
     * The object linked to the layer.
     */
    private String objectUri;
    
    /**
     * @example http://www.opensilex.org/vocabulary/oeso/Experiment
     */
    private String objectType;
    
    /**
     * @example true if the layer has all descendants, false if only the direct
     * ones
     */
    private String depth; 
    
    /**
     * @example true if the file has to be generated, false otherwise
     */
    private String generateFile;
    
    @Override
    public Object createObjectFromDTO() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Required
    @URL
    @ApiModelProperty(example = "http://www.phenome-fppn.fr/diaphen/DIA2017-1")
    public String getObjectUri() {
        return objectUri;
    }

    public void setObjectUri(String objectUri) {
        this.objectUri = objectUri;
    }

    @Required
    @ApiModelProperty(example = "true")
    public String getDepth() {
        return depth;
    }

    public void setDepth(String depth) {
        this.depth = depth;
    }

    @Required
    @URL
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SCIENTIFIC_OBJECT_TYPE)
    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }    
    
    @ApiModelProperty(example = "true")
    public String getGenerateFile() {
        return generateFile;
    }

    public void setGenerateFile(String generateFile) {
        this.generateFile = generateFile;
    }
}
