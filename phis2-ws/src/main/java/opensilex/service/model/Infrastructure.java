//******************************************************************************
//                               Infrastructure.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 5 Sept. 2018
// Contact: vincent.migot@inra.fr anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

/**
 * Infrastructure model.
 * @author Vincent Migot <vincent.migot@inra.fr>
 */
public class Infrastructure extends RdfResourceDefinition {
    
    /**
     * Infrastructure type.
     * @example http://www.opensilex.org/vocabulary/oeso#LocalInfrastructure
     */
    private String rdfType;

    /**
     * Infrastructure type label.
     * @example European Infrastructure
     */
    private String rdfTypeLabel;
    
    /**
     * URI of the parent infrastructure if it exists. 
     * @example https://emphasis.plant-phenotyping.eu 
     */
    private String parent;
    
    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    public String getRdfTypeLabel() {
        return rdfTypeLabel;
    }

    public void setRdfTypeLabel(String rdfTypeLabel) {
        this.rdfTypeLabel = rdfTypeLabel;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }
}