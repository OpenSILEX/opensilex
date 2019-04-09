//******************************************************************************
//                        ConfigurationFilesMetadataDTO.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: March 2017
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.Pattern;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;

/**
 * Configuration files metadata DTO.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
@ApiModel
public class ConfigurationFilesMetadataDTO extends AbstractVerifiedClass {
    private String provider;
    private String clientPath;
    private String plateform;
    private String fromIP;
    private String device;
    private String filename;
    private String serverFilename;
    private String extension;
    private String checksum;
    

    @ApiModelProperty(example = "test/dzdz/dzdz")
    public String getClientPath() {
        return clientPath;
    }

    public void setClientPath(String clientPath) {
        this.clientPath = clientPath;
    }
    
    public String getServerFilename() {
        return serverFilename;
    }

    public void setServerFilename(String serverFilename) {
        this.serverFilename = serverFilename;
    }
    
    @Required
    @ApiModelProperty(example = "106fa487baa1728083747de1c6df73e9")
    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    @Required
    @ApiModelProperty(example = "m3p")
    public String getPlateform() {
        return plateform;
    }

    public void setPlateform(String plateform) {
        this.plateform = plateform;
    }
    
    @Required
    @Pattern(regexp = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$")
    @ApiModelProperty(example = "147.99.7.11")
    public String getFromIP() {
        return fromIP;
    }

    public void setFromIP(String fromIP) {
        this.fromIP = fromIP;
    }

    @ApiModelProperty(example = "Tablet computer")
    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }
    
    @Required
    @ApiModelProperty(example = "mistea")
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }

    @Required
    @ApiModelProperty(example = "test numero 50005")
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    @Required
    @ApiModelProperty(example = "jpg")
    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }
    
    @Override
    public Object createObjectFromDTO() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
