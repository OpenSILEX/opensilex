/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.dal;

import java.util.HashMap;
import java.util.Map;
import org.apache.jena.datatypes.xsd.XSDDatatype;

/**
 *
 * @author vmigot
 */
public enum VueDatatypeComponents {
    BOOLEAN(XSDDatatype.XSDboolean.getURI(), "opensilex-XSDBooleanInput", "opensilex-XSDBooleanView", "datatypes.boolean"),
    NUMBER(new String[]{
        XSDDatatype.XSDbyte.getURI(),
        XSDDatatype.XSDunsignedByte.getURI(),
        XSDDatatype.XSDinteger.getURI(),
        XSDDatatype.XSDinteger.getURI(),
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
    }, "opensilex-XSDIntegerInput", "opensilex-XSDRawView", "datatypes.number"),
    DATE(XSDDatatype.XSDdate.getURI(), "opensilex-XSDDateInput", "opensilex-XSDDateView", "datatypes.date"),
    DATETIME(XSDDatatype.XSDdateTime.getURI(), "opensilex-XSDDateTimeInput", "opensilex-XSDDateTimeView", "datatypes.datetime"),
    DECIMAL(new String[]{
        XSDDatatype.XSDdecimal.getURI(),
        XSDDatatype.XSDdouble.getURI(),
        XSDDatatype.XSDfloat.getURI()

    }, "opensilex-XSDDecimalInput", "opensilex-XSDRawView", "datatypes.decimal"),
    STRING(XSDDatatype.XSDstring.getURI(), "opensilex-XSDStringInput", "opensilex-XSDRawView", "datatypes.string"),
    URI(XSDDatatype.XSDanyURI.getURI(), "opensilex-XSDUriInput", "opensilex-XSDUriView", "datatypes.uri");

    VueDatatypeComponents(String uri, String intputComponent, String viewComponent, String label) {
        this(new String[]{uri}, intputComponent, viewComponent, label);
    }

    VueDatatypeComponents(String[] uris, String intputComponent, String viewComponent, String label) {
        this.uris = uris;
        this.intputComponent = intputComponent;
        this.viewComponent = viewComponent;
        this.label = label;
    }

    private final String[] uris;

    private final String intputComponent;

    private final String viewComponent;

    private final String label;

    public String[] getUris() {
        return uris;
    }

    public String getIntputComponent() {
        return intputComponent;
    }

    public String getViewComponent() {
        return viewComponent;
    }

    public String getLabel() {
        return label;
    }

    private final static Map<String, VueDatatypeComponents> datatypeComponentsMap = new HashMap<>(VueDatatypeComponents.values().length);

    static {
        for (VueDatatypeComponents datatypeComponent : VueDatatypeComponents.values()) {
            for (String uri : datatypeComponent.getUris()) {
                datatypeComponentsMap.put(uri, datatypeComponent);
            }
        }
    }

    public static Map<String, VueDatatypeComponents> getMap() {
        return datatypeComponentsMap;
    }

}
