//**********************************************************************************************
//                                       Session.java 
//
// Author(s): Samuël Chérimont, Arnaud Charleroy
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2015
// Creation date: november 2015
// Contact:arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  March, 2018
// Subject: represents user session class
//***********************************************************************************************

package phis2ws.service.authentication;

import com.nimbusds.jwt.JWTClaimsSet;
import phis2ws.service.model.User;

/**
 * Reprents user web service session
 *
 * @version1.0
 *
 * @author Samuël Chérimont
 * @date 25/11/2015
 * @note Fields dateStart and dateEnd are not used
 * @update Arnaud Charleroy Define SQLDBModel and unique fields
 * @update Arnaud Charleroy Implementation of Json web token
 * @see https://self-issued.info/docs/draft-ietf-oauth-json-web-token.html#rfc.section.4.1.1
 */
public class Session {
    
    // Session start date
    private String dateStart;
    // Session end date
    private String dateEnd;
    // Session identifier
    private String id;
    // User name
    private String name;
    // User's session
    private User user;
    // list of claims send (PAYLOAD:DATA) in case of Json Web Token authentication
    private JWTClaimsSet jwtClaimsSet;

    public Session() {
    }

    public Session(String dateStart, String dateEnd, String id, String name) {
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;
        this.id = id;
        this.name = name;
        this.user = new User(name);
    }

    public User getUser() {
        return user;
    }

    public String getDateStart() {
        return dateStart;
    }

    public String getDateEnd() {
        return dateEnd;
    }

    /**
     * Session() - Initialise tous les champs de l'objet Session
     *
     * @param id Identifiant de session
     * @param name Nom de l'utilisateur
     *
     * @see setDateStart(),setDateEnd(), setFamilyName(), setId()
     * @date 25/11/2015
     * @update AC 07/16 Modification utilisation des attributs privés plutôt que
     * de passer par des setters publiques
     */
    public Session(String id, String name) {
        this.dateStart = null;
        this.dateEnd = null;
        this.name = name;
        this.id = id;
        this.user = new User(name);
    }

    public Session(String id, String name, User u) {
        this.dateStart = null;
        this.dateEnd = null;
        this.name = name;
        this.id = id;
        this.user = u;
    }

    /**
     * getId() - Récupère l'identifiant ed la session
     *
     * @return l'identifiant de session
     * @date 25/11/2015
     */
    public String getId() {
        return this.id;
    }

    /**
     * getFamilyName() - Récupère le nom de l'utilisateur correspondant à la
     * session représentée par l'instanec de Session
     *
     * @return le nom de l'utilisateur
     * @date 25/11/2015
     */
    public String getName() {
        return this.name;
    }

    /**
     * setDateStart() - Initialise le champ dateStart d'une instance de session
     * avec la date de connection de l'utilisateur
     *
     * @param dateStart Date de connection
     *
     * @date 25/11/2015
     * @note Inutilisée pour le moment
     */
    public void setDateStart(String dateStart) {
        this.dateStart = dateStart;
    }

    /**
     * setDateEnd() - Initialise le champ dateEnd d'une instance de Session avec
     * la date de fin de connection de l'utilisateur
     *
     * @param dateEnd date de fin de session
     *
     * @date 25/11/2015
     * @note Inutilisée pour le moment
     */
    public void setDateEnd(String dateEnd) {
        this.dateEnd = dateEnd;
    }

    /**
     * setid() - Initialise le champ id d'une instance de Session avec un
     * identifiant de session
     *
     * @param id identifiant de session
     * @date 25/11/2015
     */
    public void setId(String id) {
        this.id = id;
    }

    
    public void setName(String name) {
        this.name = name;
    }

    //SILEX:conception
    // Key payload during session if we need additionnal informations
    public JWTClaimsSet getJwtClaimsSet() {
        return jwtClaimsSet;
    }

    public void setJwtClaimsSet(JWTClaimsSet jwtClaimsSet) {
        this.jwtClaimsSet = jwtClaimsSet;
    }
    //\SILEX:conception
}
