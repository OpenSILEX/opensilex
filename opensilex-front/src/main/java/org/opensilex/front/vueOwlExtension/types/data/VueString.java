/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.types.data;

import java.net.URI;
import java.util.List;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.opensilex.front.vueOwlExtension.types.VueOntologyDataType;

/**
 *
 * @author vmigot
 */
public class VueString implements VueOntologyDataType {

    @Override
    public String getTypeUri() {
        return XSDDatatype.XSDstring.getURI();
    }

    @Override
    public String getInputComponent() {
        return "opensilex-XSDStringInput";
    }

    @Override
    public String getViewComponent() {
        return "opensilex-XSDRawView";
    }

    @Override
    public String getLabelKey() {
        return "datatypes.string";
    }

}
