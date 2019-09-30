/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.security.user;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.annotations.SPARQLResourceURI;
import javax.mail.internet.InternetAddress;

/**
 *
 * @author Vincent Migot
 */
@SPARQLResource(
        ontology = UserOntology.class,
        resource = "User",
        graph = UserOntology.GRAPH_USER
)
public class User implements Principal {

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
}
