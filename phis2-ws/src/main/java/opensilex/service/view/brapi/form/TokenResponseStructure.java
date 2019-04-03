//******************************************************************************
//                           TokenResponseStructure.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 25 Nov 2015
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.view.brapi.form;

import opensilex.service.view.brapi.Metadata;

/**
 * BrAPI Token response structure.
 * @author Samuël Chérimont
 */
public class TokenResponseStructure {

    private final Metadata metadata;
    
    private final String session_token;

    /**
     * Adds the session id to a new metadata object.
     * @param sessionId
     */
    public TokenResponseStructure(String sessionId) {
        this.metadata = new Metadata(0, 0, 1);
        this.session_token = sessionId;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public String getSession_token() {
        return session_token;
    }
}
