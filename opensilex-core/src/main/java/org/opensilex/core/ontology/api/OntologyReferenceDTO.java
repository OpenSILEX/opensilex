//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, morgane.vidal@inra.fr,anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.ontology.api;

import java.net.URI;
import org.opensilex.core.ontology.OntologyReference;


/**
 * Ontology reference model DTO.
 * @author Morgane Vidal
 */
public class OntologyReferenceDTO {
    
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
    
    public static OntologyReferenceDTO fromModel(OntologyReference model) {
        OntologyReferenceDTO dto = new OntologyReferenceDTO();

        dto.setObject(model.getObject());
        dto.setProperty(model.getProperty());
        dto.setSeeAlso(model.getSeeAlso());
        
        return dto;
    }
    
    public static OntologyReference toModel(OntologyReferenceDTO dto) {
        OntologyReference model = new OntologyReference();

        model.setObject(dto.getObject());
        model.setProperty(dto.getProperty());
        model.setSeeAlso(dto.getSeeAlso());
        
        return model;
    }

}
