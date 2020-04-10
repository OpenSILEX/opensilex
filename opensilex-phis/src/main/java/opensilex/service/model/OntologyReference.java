//******************************************************************************
//                          OntologyReference.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: November, 15 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import io.swagger.annotations.ApiModelProperty;
import opensilex.service.resource.validation.interfaces.URL;

/**
 * Ontology reference model.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class OntologyReference {
    
    /**
     * @example skos:exactMatch
     */
    private String property;
    
    /***
     * @example http://www.cropontology.org/rdf/CO_715:0000139
     */
    private String object;
    
    /**
     * @example http://www.cropontology.org/ontology/CO_715/
     */
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
