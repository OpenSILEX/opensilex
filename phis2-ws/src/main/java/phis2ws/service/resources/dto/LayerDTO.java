//**********************************************************************************************
//                                       LayerDTO.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: August, 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  August, 16 2017
// Subject: A class which contains methods to automatically check the attributes
//          of a class, from rules defined by user.
//          Contains the list of the elements which might be send by the client to
//          create a geojson file corresponding to a layer
//***********************************************************************************************
package phis2ws.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.resources.validation.interfaces.URL;

public class LayerDTO extends AbstractVerifiedClass {
    final static Logger LOGGER = LoggerFactory.getLogger(LayerDTO.class);
    
    /**
     * @param objectUri l'uri de l'objet pour lequel on veut une couche
     * @param objectType type de l'objet (ex : http://www.phenome-fppn.fr/Vocabulary/2017/Experiment)
     * @param depth true si on souhaite avoir tous les déscendants représentés dans
     *                   la couche,
     *              false si on veut juste l'objet et ses enfants directs
     * @param generateFile true si on génère le geojson, false si on ne génère pas le geojson
     */
    private String objectUri; 
    private String objectType;
    private String depth; 
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
