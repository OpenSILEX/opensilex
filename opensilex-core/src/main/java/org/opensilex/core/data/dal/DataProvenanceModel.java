//******************************************************************************
//                          DataProvenanceModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.dal;

import java.net.URI;
import java.util.List;
import org.bson.Document;

/**
 * Provenance model used in DataModel
 * @author Alice Boizet
 */
public class DataProvenanceModel {
    URI uri;
    List<ProvEntityModel> provUsed; 
    Document settings; 

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public List<ProvEntityModel> getProvUsed() {
        return provUsed;
    }

    public void setProvUsed(List<ProvEntityModel> provUsed) {
        this.provUsed = provUsed;
    }

    public Document getSettings() {
        return settings;
    }

    public void setSettings(Document settings) {
        this.settings = settings;
    }
    
}
