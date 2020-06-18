/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import java.util.List;

/**
 *
 * @author vince
 */
public class ClassPropertiesDTO {

    List<DatatypePropertyDTO> datatypeProperties;

    List<ObjectPropertyDTO> objectProperties;

    public List<DatatypePropertyDTO> getDatatypeProperties() {
        return datatypeProperties;
    }

    public void setDatatypeProperties(List<DatatypePropertyDTO> datatypeProperties) {
        this.datatypeProperties = datatypeProperties;
    }

    public List<ObjectPropertyDTO> getObjectProperties() {
        return objectProperties;
    }

    public void setObjectProperties(List<ObjectPropertyDTO> objectProperties) {
        this.objectProperties = objectProperties;
    }

}
