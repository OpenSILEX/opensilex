/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.dal.user;

import java.net.URI;
import javax.mail.internet.InternetAddress;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.server.security.AuthenticationService;
import org.opensilex.server.security.user.User;
import org.opensilex.sparql.SPARQLService;
import org.opensilex.sparql.exceptions.SPARQLEntityNotFoundException;

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

    public User getByEmail(InternetAddress email) throws Exception {
        try {
            return sparql.getByUniquePropertyValue(
                    User.class,
                    FOAF.mbox,
                    email
            );
        } catch (SPARQLEntityNotFoundException ex) {
            return null;
        }
    }

    public User create(
            InternetAddress email,
            String firstName,
            String lastName,
            String password
    ) throws Exception {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        
        if (password != null) {
            user.setPasswordHash(authentication.getPasswordHash(password));
        }

        //TODO create URI
        user.setUri(new URI("http://DFSDFSSF/"));

        sparql.create(user);

        return user;
    }

    public boolean authenticate(User user, String password) throws Exception {
        return (user != null && authentication.checkPassword(password, user.getPasswordHash()));
    }

    public boolean userEmailexists(InternetAddress email) throws Exception {
        return sparql.existsByUniquePropertyValue(
                User.class,
                FOAF.mbox,
                email
        );
    }

    public User getByURI(URI uri) throws Exception {
        return sparql.getByURI(User.class, uri);
    }

}
