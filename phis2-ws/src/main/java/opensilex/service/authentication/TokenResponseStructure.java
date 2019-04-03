//******************************************************************************
//                                 Session.java 
// SILEX-PHIS
// Copyright © INRA 2015
// Creation date: 25 November 2015
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.authentication;

import opensilex.service.PropertiesFileManager;
import opensilex.service.view.brapi.Metadata;

/**
 * Structure of a response representing a token corresponding to the BrAPI 
 * prerequisites. 
 * @author Samuël Chérimont
 */
public class TokenResponseStructure {

    private final Metadata metadata;
    private final String userDisplayName;
    private final String access_token;
    private final String expires_in;

    /**
     * Prepares an empty metadata object and joins the session id to the session 
     * token variable.
     * @param sessionId
     * @param userDisplayName
     */
    public TokenResponseStructure(String sessionId, String userDisplayName) {
        this.metadata = new Metadata(0, 0, 1);
        this.userDisplayName = userDisplayName;
        this.access_token = sessionId;
        this.expires_in = PropertiesFileManager.getConfigFileProperty("service", "sessionTime");
    }

    public TokenResponseStructure(String id, String userDisplayName, String expires_in) {
        this.metadata = new Metadata(0, 0, 1);
        this.userDisplayName = userDisplayName;
        this.access_token = id;
        if (expires_in == null) {
            this.expires_in = PropertiesFileManager.getConfigFileProperty("service", "sessionTime");
        } else {
            this.expires_in = expires_in;
        }
    }
}
