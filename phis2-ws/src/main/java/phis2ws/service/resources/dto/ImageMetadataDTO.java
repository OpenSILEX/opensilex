//**********************************************************************************************
//                                       ImageMetadata.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: December, 8 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  December, 8 2017
// Subject: Represents the submitted JSON for the images
//***********************************************************************************************
package phis2ws.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;

public class ImageMetadataDTO extends AbstractVerifiedClass {
    
    private String rdfType;
    private List<ConcernItemDTO> concern;
    private List<ShootingConfigurationDTO> configuration;
    private List<FileInformationDTO> fileInfo;

    @Override
    public Map rules() {
         Map<String, Boolean> rules = new HashMap<>();
        rules.put("rdfType", Boolean.TRUE);
        return rules;
    }

    @Override
    public Object createObjectFromDTO() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_IMAGE_TYPE)
    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    public List<ConcernItemDTO> getConcern() {
        return concern;
    }

    public void setConcern(List<ConcernItemDTO> concern) {
        this.concern = concern;
    }

    public List<ShootingConfigurationDTO> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(List<ShootingConfigurationDTO> configuration) {
        this.configuration = configuration;
    }

    public List<FileInformationDTO> getFileInfo() {
        return fileInfo;
    }

    public void setFileInfo(List<FileInformationDTO> fileInfo) {
        this.fileInfo = fileInfo;
    }
}
