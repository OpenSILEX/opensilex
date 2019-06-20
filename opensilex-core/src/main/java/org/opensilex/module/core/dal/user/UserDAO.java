/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.dal.user;

import javax.mail.internet.InternetAddress;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.module.core.service.sparql.SPARQLService;

/**
 * @author vincent
 */
public class UserDAO {

    private SPARQLService sparql;
    
    public UserDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public User getByEmail(InternetAddress email) {
        try {
            return sparql.getByUniquePropertyValue(
                    User.class,
                    FOAF.mbox,
                    email
            );
        } catch (Exception ex) {
            // TODO log error & throw some exception
            return null;
        }
    }
}
