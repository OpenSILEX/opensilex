/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.data.dal;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author sammy
 */
@PersistenceCapable(embeddedOnly="true")
public class DataScientificObjectModel {
    @Column(name="uri")
    URI uri;

    @JsonProperty("rdf:type")
    @Column(name="rdf:type")
    URI type;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }
}
