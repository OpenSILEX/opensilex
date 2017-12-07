package phis2ws.service.view.brapi.form;

import phis2ws.service.view.brapi.Metadata;

/**
 * TokenResponseStructure - Prépare le résultat final pour qu'il soit conforme au modèle de
 la Plant breeding API
 *
 * @version1.0
 *
 * @author Samuël Chérimont
 * @date 25/11/2015
 * @note session_token n'est pas conforme aux norme de nomination Java
 */
public class TokenResponseStructure {

    private final Metadata metadata;
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
