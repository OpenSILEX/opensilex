//******************************************************************************
//                                 Session.java 
// SILEX-PHIS
// Copyright © INRA 2015
// Creation date: November 2015
// Contact:arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.authentication;

import opensilex.service.PropertiesFileManager;
import opensilex.service.view.brapi.Metadata;

/**
 * TokenResponseStructure - Prépare le résultat final pour qu'il soit conforme
 * au modèle de la Plant breeding API
 *
 * @version1.0
 *
 * @author Samuël Chérimont
 * @date 25/11/2015
 * @note session_token n'est pas conforme aux norme de nomination Java
 */
public class TokenResponseStructure {

    private final Metadata metadata;
    private final String userDisplayName;
    private final String access_token;
    private final String expires_in;

    /**
     * ResultForm() - Prepare l'objet métadata vide et associe l'identifiant de
     * session à la variable session_token
     *
     * @param id L'identifiant de session
     * @param userDisplayName nom et prénome de l'utilisateur
     *
     * @see result_output.Metadata
     * @date 25/11/2015
     * @update 08/2016
     */
    public TokenResponseStructure(String id, String userDisplayName) {
        this.metadata = new Metadata(0, 0, 1);
        this.userDisplayName = userDisplayName;
        this.access_token = id;
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
