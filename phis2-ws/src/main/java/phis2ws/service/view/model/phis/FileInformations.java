//**********************************************************************************************
//                                       FileInformations.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: December, 11 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  December, 11 2017
// Subject: Represents the file informations view
//***********************************************************************************************
package phis2ws.service.view.model.phis;

/**
 * the file informations view
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class FileInformations {
    
    //md5sum of the file
    private String checksum;
    //extension of the file (e.g. PNG)
    private String extension;
    //file path of the file on the server
    private String serverFilePath;

    public FileInformations() {
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getServerFilePath() {
        return serverFilePath;
    }

    public void setServerFilePath(String serverFilePath) {
        this.serverFilePath = serverFilePath;
    }
}
