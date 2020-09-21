//******************************************************************************
//                          DataProvenanceModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.dal;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.jdo.annotations.Cacheable;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Provenance model used in DataModel
 * @author Alice Boizet
 */
@PersistenceCapable(embeddedOnly="true")
public class DataProvenanceModel {
    @Column(name="provenanceUri")
    URI provenanceUri;
    
    @Element(embeddedMapping={
    @Embedded(members={
        @Persistent(name="uri", column="uri"),
        @Persistent(name="type", column="rdf:type")})
    })
    @Join(column="uri")
    @Persistent(defaultFetchGroup="true")
    @Column(name="prov:used")
    @JsonProperty("prov:used")
    @Cacheable("false")
    List<EntityModel> provUsed;
    
    @Column(name="settings")
    @Persistent(defaultFetchGroup="true")    
    Map settings; 

    public URI getUri() {
        return provenanceUri;
    }

    public void setUri(URI uri) {
        this.provenanceUri = uri;
    }

    public List<EntityModel> getProvUsed() {
        return provUsed;
    }

    public void setProvUsed(List<EntityModel> provUsed) {
        this.provUsed = provUsed;
    }

    public Map getSettings() {
        return settings;
    }

    public void setSettings(Map settings) {
        this.settings = settings;
    }
    
}
