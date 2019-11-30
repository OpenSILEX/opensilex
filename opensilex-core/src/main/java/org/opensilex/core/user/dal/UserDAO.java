//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.user.dal;

import java.net.*;
import javax.mail.internet.*;
import org.apache.jena.sparql.vocabulary.*;
import org.opensilex.server.security.*;
import org.opensilex.server.security.model.*;
import org.opensilex.sparql.*;

/**
 * @author vincent
 */
public class UserDAO {

    private SPARQLService sparql;
    private AuthenticationService authentication;

    public UserDAO(SPARQLService sparql, AuthenticationService authentication) {
        this.sparql = sparql;
        this.authentication = authentication;
    }

    public UserModel getByEmail(InternetAddress email) throws Exception {
        return sparql.getByUniquePropertyValue(UserModel.class,
                FOAF.mbox,
                email
        );
    }

    public UserModel create(
            InternetAddress email,
            String firstName,
            String lastName,
            String password
    ) throws Exception {
        UserModel user = new UserModel();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);

        if (password != null) {
            user.setPasswordHash(authentication.getPasswordHash(password));
        }

        sparql.create(user);

        return user;
    }

    public boolean authenticate(UserModel user, String password) throws Exception {
        return (user != null && authentication.checkPassword(password, user.getPasswordHash()));
    }

    public boolean userEmailexists(InternetAddress email) throws Exception {
        return sparql.existsByUniquePropertyValue(
                UserModel.class,
                FOAF.mbox,
                email
        );
    }

    public UserModel getByURI(URI uri) throws Exception {
        return sparql.getByURI(UserModel.class, uri);
    }

}
