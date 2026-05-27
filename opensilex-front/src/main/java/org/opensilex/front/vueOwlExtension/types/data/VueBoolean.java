/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.types.data;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.opensilex.front.vueOwlExtension.types.VueOntologyDataType;

/**
 *
 * @author vmigot
 */
public class VueBoolean implements VueOntologyDataType {

    @Override
    public String getTypeUri() {
        return XSDDatatype.XSDboolean.getURI();
    }

    @Override
    public String getInputComponent() {
        return "opensilex-XSDBooleanInput";
    }
    
    @Override
    public String getViewComponent() {
        return "opensilex-XSDBooleanView";
    }

    @Override
    public String getLabelKey() {
        return "datatypes.boolean";
    }

}
