//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.security.model;

import java.net.*;
import java.security.*;
import java.util.*;
import javax.mail.internet.*;
import org.apache.jena.sparql.vocabulary.*;
import org.opensilex.sparql.annotations.*;
import org.opensilex.sparql.utils.*;

/**
 *
 * @author Vincent Migot
 */
@SPARQLResource(
        ontology = UserOntology.class,
        resource = "User",
        graph = "users"
)
public class User implements Principal, ClassURIGenerator<User> {

    @SPARQLResourceURI()
    private URI uri;

    @SPARQLProperty(
            ontology = FOAF.class,
            property = "firstName",
            required = true
    )
    private String firstName;

    @SPARQLProperty(
            ontology = FOAF.class,
            property = "lastName",
            required = true
    )
    private String lastName;

    @SPARQLProperty(
            ontology = FOAF.class,
            property = "mbox",
            required = true
    )
    private InternetAddress email;

    @SPARQLProperty(
            ontology = UserOntology.class,
            property = "hasPasswordHash"
    )
    private String passwordHash;

    @SPARQLProperty(
            ontology = UserOntology.class,
            property = "hasGroup"
    )
    private List<Group> groups;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

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

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @Override
    public String getName() {
        return getFirstName() + " " + getLastName() + " <" + getEmail().toString() + ">";
    }

    @Override
    public String[] getUriSegments(User instance) {
        return new String[] {
            "agent",
            instance.getFirstName() + "_" + instance.getLastName()
        };
    }
}
