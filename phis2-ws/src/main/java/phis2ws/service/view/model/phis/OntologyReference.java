//**********************************************************************************************
//                                       OntologyReference.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: November, 15 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  November, 15 2017
// Subject: Represents the reference to an ontology view
//***********************************************************************************************
package phis2ws.service.view.model.phis;

import io.swagger.annotations.ApiModelProperty;
import phis2ws.service.resources.dto.validation.interfaces.URL;

public class OntologyReference {
    /**
     * @param property ex. skos:exactMatch
     * @param object ex. http://www.cropontology.org/rdf/CO_715:0000139
     * @param seeAlso ex. http://www.cropontology.org/ontology/CO_715/
     */
    private String property;
    private String object;
    private String seeAlso;
    
    public OntologyReference() {
        
    }

    @URL
    @ApiModelProperty(example = "http://www.w3.org/2004/02/skos/core#closeMatch")
    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }
    
    @URL
    @ApiModelProperty(example = "http://www.cropontology.org/rdf/CO_715:0000139")
    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }
    
    @URL
    @ApiModelProperty(example = "http://www.cropontology.org/ontology/CO_715/")
    public String getSeeAlso() {
        return seeAlso;
    }

    public void setSeeAlso(String seeAlso) {
        this.seeAlso = seeAlso;
    }
}
