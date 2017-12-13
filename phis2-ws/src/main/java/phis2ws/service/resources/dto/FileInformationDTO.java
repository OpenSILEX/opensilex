//**********************************************************************************************
//                                       FileInformationDTO.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: December, 8 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  December, 8 2017
// Subject: Represents the JSON submitted for the file informations 
//***********************************************************************************************
package phis2ws.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.Map;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.view.model.phis.FileInformations;

public class FileInformationDTO extends AbstractVerifiedClass {

    private String checksum;
    private String extension;
    
    @Override
    public Map rules() {
        Map<String, Boolean> rules = new HashMap<>();
        rules.put("checksum", Boolean.TRUE);
        rules.put("extension", Boolean.TRUE);
        return rules;
    }

    @Override
    public FileInformations createObjectFromDTO() {
        FileInformations fileInformations = new FileInformations();
        fileInformations.setChecksum(checksum);
        fileInformations.setExtension(extension);
        return fileInformations;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_FILE_INFORMATION_CHECKSUM)
    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_FILE_INFORMATION_EXTENSION)
    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }
}
