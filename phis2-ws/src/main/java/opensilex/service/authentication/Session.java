//******************************************************************************
//                                 Session.java 
// SILEX-PHIS
// Copyright © INRA 2015
// Creation date: 25 November 2015
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.authentication;

import opensilex.service.model.User;

/**
 * Represents an user web service session.
 * @update [Arnaud Charleroy] Define SQLDBModel and unique fields
 * @update [Arnaud Charleroy] Implementation of Json web token
 * @update [Vincent Migot] Convert as an interface to use in modularity
 * @author Samuël Chérimont
 */
public interface Session {
    
    public User getUser();
}
