/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.datatypes;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vmigot
 */
public enum PrimitiveDatatypeComponents {
    BOOLEAN(XSDDatatype.XSDboolean.getURI(), "opensilex-XSDBooleanInput", "opensilex-XSDBooleanView"),
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
    }, "opensilex-XSDIntegerInput", "opensilex-XSDRawView"),
    DATE(XSDDatatype.XSDdate.getURI(), "opensilex-XSDDateInput", "opensilex-XSDDateView"),
    DATETIME(XSDDatatype.XSDdateTime.getURI(), "opensilex-XSDDateTimeInput", "opensilex-XSDDateTimeView"),
    DECIMAL(new String[]{
        XSDDatatype.XSDdecimal.getURI(),
        XSDDatatype.XSDdouble.getURI(),
        XSDDatatype.XSDfloat.getURI()

    }, "opensilex-XSDDecimalInput", "opensilex-XSDRawView"),
    STRING(XSDDatatype.XSDstring.getURI(), "opensilex-XSDStringInput", "opensilex-XSDRawView"),
    URI(XSDDatatype.XSDanyURI.getURI(), "opensilex-XSDUriInput", "opensilex-XSDUriView");

    PrimitiveDatatypeComponents(String uri, String intputComponent, String viewComponent) {
        this(new String[]{uri}, intputComponent, viewComponent);
    }

    PrimitiveDatatypeComponents(String[] uris, String intputComponent, String viewComponent) {
        this.uris = uris;
        this.intputComponent = intputComponent;
        this.viewComponent = viewComponent;
    }

    private final String[] uris;

    private final String intputComponent;

    private final String viewComponent;

    public String[] getUris() {
        return uris;
    }

    public String getIntputComponent() {
        return intputComponent;
    }

    public String getViewComponent() {
        return viewComponent;
    }

    private final static Map<String, DatatypeComponents> datatypeComponentsMap = new HashMap<>(PrimitiveDatatypeComponents.values().length);

    static {
        for (PrimitiveDatatypeComponents datatypeComponent : PrimitiveDatatypeComponents.values()) {
            for (String uri : datatypeComponent.getUris()) {
                DatatypeComponents dtc = DatatypeComponents.fromString(uri, datatypeComponent.getIntputComponent(), datatypeComponent.getViewComponent());
                if (dtc != null) {
                    datatypeComponentsMap.put(uri, dtc);
                }
            }
        }
    }

    public static Map<String, DatatypeComponents> getMap() {
        return datatypeComponentsMap;
    }
}
