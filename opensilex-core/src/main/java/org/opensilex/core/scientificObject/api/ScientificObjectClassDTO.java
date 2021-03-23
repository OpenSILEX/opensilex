/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.api;

import org.opensilex.core.ontology.api.RDFTypeDTO;

/**
 *
 * @author vince
 */
public class ScientificObjectClassDTO extends RDFTypeDTO {

    private boolean isAbstractClass;

    public boolean getIsAbstractClass() {
        return isAbstractClass;
    }

    public void setIsAbstractClass(boolean isAbstractClass) {
        this.isAbstractClass = isAbstractClass;
    }
}
