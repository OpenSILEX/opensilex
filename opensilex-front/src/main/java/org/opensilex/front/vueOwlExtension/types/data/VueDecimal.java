/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.types.data;

import java.util.Arrays;
import java.util.List;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.opensilex.front.vueOwlExtension.types.VueOntologyDataType;

/**
 *
 * @author vmigot
 */
public class VueDecimal implements VueOntologyDataType {

    @Override
    public String getTypeUri() {
        return XSDDatatype.XSDdecimal.getURI();
    }

    @Override
    public List<String> getTypeUriAliases() {
        return Arrays.asList(new String[]{
            XSDDatatype.XSDdouble.getURI(),
            XSDDatatype.XSDfloat.getURI()
        });
    }

    @Override
    public String getInputComponent() {
        return "opensilex-XSDDecimalInput";
    }

    @Override
    public String getViewComponent() {
        return "opensilex-XSDNumberView";
    }

    @Override
    public String getLabelKey() {
        return "datatypes.decimal";
    }

}
