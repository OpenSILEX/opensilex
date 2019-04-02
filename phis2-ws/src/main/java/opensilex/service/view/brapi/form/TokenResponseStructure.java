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
    //@note session_token n'est pas conforme aux norme de nomination Java
    private final String session_token;

    /**
     * ResultForm() - Prepare l'objet métadata vide et associe l'identifiant de
     * session à la variable session_token
     *
     * @param id L'identifiant de session
     *
     * @see result_output.Metadata
     * @date 25/11/2015
     */
    public TokenResponseStructure(String id) {
        this.metadata = new Metadata(0, 0, 1);
        this.session_token = id;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public String getSession_token() {
        return session_token;
    }

}
