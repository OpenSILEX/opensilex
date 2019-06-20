/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.dal.user;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.auth0.jwt.algorithms.Algorithm;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.module.core.service.sparql.annotations.SPARQLProperty;
import org.opensilex.module.core.service.sparql.annotations.SPARQLResource;
import org.opensilex.module.core.service.sparql.annotations.SPARQLResourceURI;
import javax.mail.internet.InternetAddress;
import org.apache.jena.vocabulary.DC;
import org.opensilex.module.core.ontology.Oeso;

/**
 *
 * @author vincent
 */
@SPARQLResource(
        ontology = FOAF.class,
        resource = "Person"
)
public class User {

    @SPARQLResourceURI()
    private URI uri;

    @SPARQLProperty(
            ontology = FOAF.class,
            property = "mbox",
            required = true
    )
    private InternetAddress email;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasGroup"
    )
    private List<Group> groups;
        
    public URI getURI() {
        return uri;
    }

    public InternetAddress getEmail() {
        return email;
    }

    public String getName() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public String getPasswordHash() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean checkPassword(String password) {
        return BCrypt.verifyer().verify(password.toCharArray(), getPasswordHash()).verified;
    }

    //--------------------------------------------------------------------------
    // TODO: move everything below in some token manager class
    //--------------------------------------------------------------------------
    static {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.generateKeyPair();
            RSAPublicKey publicKey = (RSAPublicKey) kp.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) kp.getPrivate();
            algoRSA = Algorithm.RSA512(publicKey, privateKey);
        } catch (NoSuchAlgorithmException ex) {
            // Never append
        }
    }

    private static Algorithm algoRSA = null;

    public String computeToken() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
