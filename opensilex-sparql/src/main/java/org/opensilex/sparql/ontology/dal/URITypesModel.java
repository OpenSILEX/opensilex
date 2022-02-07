/*******************************************************************************
 *                         URITypesModel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.sparql.ontology.dal;

import java.net.URI;
import java.util.List;
import org.opensilex.sparql.model.SPARQLModel;

/**
 *
 * @author Alice Boizet
 */
public class URITypesModel implements SPARQLModel {
    
    protected URI uri;

    protected List<URI> rdfTypes;

    public URITypesModel(URI uri, List<URI> rdfTypes) {
        this.uri = uri;
        this.rdfTypes = rdfTypes;
    }    

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public List<URI> getRdfTypes() {
        return rdfTypes;
    }

    public void setRdfTypes(List<URI> rdfTypes) {
        this.rdfTypes = rdfTypes;
    }

}
