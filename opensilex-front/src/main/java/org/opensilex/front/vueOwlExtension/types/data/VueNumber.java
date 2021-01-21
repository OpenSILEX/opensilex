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
public class VueNumber implements VueOntologyDataType {

    @Override
    public String getTypeUri() {
        return XSDDatatype.XSDinteger.getURI();
    }

    @Override
    public List<String> getTypeUriAliases() {
        return Arrays.asList(new String[]{
            XSDDatatype.XSDbyte.getURI(),
            XSDDatatype.XSDunsignedByte.getURI(),
            XSDDatatype.XSDint.getURI(),
            XSDDatatype.XSDunsignedInt.getURI(),
            XSDDatatype.XSDnegativeInteger.getURI(),
            XSDDatatype.XSDnonNegativeInteger.getURI(),
            XSDDatatype.XSDpositiveInteger.getURI(),
            XSDDatatype.XSDnonPositiveInteger.getURI(),
            XSDDatatype.XSDpositiveInteger.getURI(),
            XSDDatatype.XSDpositiveInteger.getURI(),
            XSDDatatype.XSDpositiveInteger.getURI(),
            XSDDatatype.XSDlong.getURI(),
            XSDDatatype.XSDunsignedLong.getURI(),
            XSDDatatype.XSDshort.getURI(),
            XSDDatatype.XSDunsignedShort.getURI()
        });
    }

    @Override
    public String getInputComponent() {
        return "opensilex-XSDIntegerInput";
    }

    @Override
    public String getViewComponent() {
        return "opensilex-XSDNumberView";
    }

    @Override
    public String getLabelKey() {
        return "datatypes.number";
    }

}
