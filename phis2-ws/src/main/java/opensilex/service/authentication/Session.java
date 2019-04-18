//******************************************************************************
//                                 Session.java 
// SILEX-PHIS
// Copyright © INRA 2015
// Creation date: 25 November 2015
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.authentication;

import com.nimbusds.jwt.JWTClaimsSet;
import opensilex.service.model.User;

/**
 * Represents an user web service session.
 * @update [Arnaud Charleroy] Define SQLDBModel and unique fields
 * @update [Arnaud Charleroy] Implementation of Json web token
 * @see https://self-issued.info/docs/draft-ietf-oauth-json-web-token.html#rfc.section.4.1.1
 * @author Samuël Chérimont
 */
public class Session {
    
    /**
     * Start date.
     * @note Fields dateStart and dateEnd are not used
     */
    private String dateStart;
    
    /**
     * End date.
     * @note Fields dateStart and dateEnd are not used
     */
    private String dateEnd;
    
    /**
     * Identifier.
     */
    private String id;
    
    /**
     * User name.
     */
    private String userName;
    
    /**
     * Session's user.
     */
    private User user;
    
    /**
     * List of claims sent (PAYLOAD:DATA) in case of JSON web token authentication.
     */
    private JWTClaimsSet jwtClaimsSet;

    public Session(String dateStart, String dateEnd, String id, String userName) {
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;
        this.id = id;
        this.userName = userName;
        this.user = new User(userName);
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
     * Constructor.
     * @param id
     * @param userName
     */
    public Session(String id, String userName) {
        this.dateStart = null;
        this.dateEnd = null;
        this.userName = userName;
        this.id = id;
        this.user = new User(userName);
    }

    public Session(String id, String name, User u) {
        this.dateStart = null;
        this.dateEnd = null;
        this.userName = name;
        this.id = id;
        this.user = u;
    }
    
    public String getId() {
        return this.id;
    }
    
    public String getName() {
        return this.userName;
    }
    
    public void setDateStart(String dateStart) {
        this.dateStart = dateStart;
    }
    
    public void setDateEnd(String dateEnd) {
        this.dateEnd = dateEnd;
    }
    
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.userName = name;
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
