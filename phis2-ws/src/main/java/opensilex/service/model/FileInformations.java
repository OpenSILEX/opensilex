//******************************************************************************
//                              FileInformations.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 11 December 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

/**
 * File information model.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class FileInformations {
    
    /**
     * md5sum of the file.
     */
    private String checksum;
    
    /**
     * Extension of the file.
     * @example PNG
     */
    private String extension;
    
    /**
     * File path on the server.
     */
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
