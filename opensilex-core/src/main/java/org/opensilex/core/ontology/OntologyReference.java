//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, morgane.vidal@inra.fr,anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.ontology;

import java.net.*;

/**
 * Ontology reference model.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class OntologyReference {
    
    /**
     * @example skos:exactMatch
     */
    private URI property;
    
    /***
     * @example http://www.cropontology.org/rdf/CO_715:0000139
     */
    private URI object;
    
    /**
     * @example http://www.cropontology.org/ontology/CO_715/
     */
    private URI seeAlso;
    
    public URI getProperty() {
        return property;
    }

    public void setProperty(URI property) {
        this.property = property;
    }
    
    public URI getObject() {
        return object;
    }

    public void setObject(URI object) {
        this.object = object;
    }
    
    public URI getSeeAlso() {
        return seeAlso;
    }

    public void setSeeAlso(URI seeAlso) {
        this.seeAlso = seeAlso;
    }
}
