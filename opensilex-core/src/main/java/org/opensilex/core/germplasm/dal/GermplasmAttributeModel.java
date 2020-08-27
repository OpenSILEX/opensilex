/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.germplasm.dal;

import java.net.URI;
import java.util.Map;
import javax.jdo.annotations.Convert;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.persistence.Id;

/**
 *
 * @author boizetal
 */
@PersistenceCapable(table = "germplasmAttributes")
public class GermplasmAttributeModel {
    @Convert(URIStringConverter.class)
    @Id
    URI uri;
    
    @Persistent(defaultFetchGroup="true")
    Map<String, String> attribute;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public Map<String, String> getAttribute() {
        return attribute;
    }

    public void setAttribute(Map<String, String> attribute) {
        this.attribute = attribute;
    }

}
