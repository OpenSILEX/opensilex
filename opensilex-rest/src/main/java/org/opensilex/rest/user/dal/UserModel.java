//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.rest.user.dal;

import org.opensilex.rest.authentication.SecurityOntology;
import java.security.Principal;
import javax.mail.internet.InternetAddress;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

/**
 *
 * @author Vincent Migot
 */
@SPARQLResource(
        ontology = FOAF.class,
        resource = "Agent",
        graph = "users",
        prefix = "usr"
)
public class UserModel extends SPARQLResourceModel implements Principal, ClassURIGenerator<UserModel> {

    @SPARQLProperty(
            ontology = FOAF.class,
            property = "firstName",
            required = true
    )
    private String firstName;
    public static final String FIRST_NAME_FIELD = "firstName";

    @SPARQLProperty(
            ontology = FOAF.class,
            property = "lastName",
            required = true
    )
    private String lastName;
    public static final String LAST_NAME_FIELD = "lastName";

    @SPARQLProperty(
            ontology = FOAF.class,
            property = "mbox",
            required = true
    )
    private InternetAddress email;
    public static final String EMAIL_FIELD = "email";

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasPasswordHash",
            ignoreUpdateIfNull = true
    )
    private String passwordHash;

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "isAdmin"
    )
    private Boolean admin = Boolean.FALSE;

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasLang"
    )
    private String lang = "";

    private String token;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public InternetAddress getEmail() {
        return email;
    }

    public void setEmail(InternetAddress email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Boolean isAdmin() {
        return admin;
    }

    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String getName() {
        return getFirstName() + " " + getLastName() + " <" + getEmail().toString() + ">";
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    @Override
    public String[] getUriSegments(UserModel instance) {
        return new String[]{
            instance.getFirstName(),
            instance.getLastName()
        };
    }
}
