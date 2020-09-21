//******************************************************************************
//                          ExperimentProvModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.dal;

import java.net.URI;
import javax.jdo.annotations.PersistenceCapable;

/**
 * Experiment model for provenance
 * @author Alice Boizet
 */
@PersistenceCapable(embeddedOnly="true")
public class ExperimentProvModel {
    URI uri;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
    
}
