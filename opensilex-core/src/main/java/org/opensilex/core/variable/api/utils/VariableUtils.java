package org.opensilex.core.variable.api.utils;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.opensilex.core.variable.api.VariableDatatypeDTO;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public class VariableUtils {

    public static List<VariableDatatypeDTO> getVariablesXsdTypes() throws URISyntaxException {
        return Arrays.asList(
                new VariableDatatypeDTO(XSDDatatype.XSDboolean, "datatypes.boolean"),
                new VariableDatatypeDTO(XSDDatatype.XSDdate, "datatypes.date"),
                new VariableDatatypeDTO(XSDDatatype.XSDdateTime, "datatypes.datetime"),
                new VariableDatatypeDTO(XSDDatatype.XSDdecimal, "datatypes.decimal"),
                new VariableDatatypeDTO(XSDDatatype.XSDinteger, "datatypes.number"),
                new VariableDatatypeDTO(XSDDatatype.XSDstring, "datatypes.string")
        );
    }

    public static List<URI> getDataTypesURIs() throws URISyntaxException {
        return getVariablesXsdTypes().stream().map(VariableDatatypeDTO::getUri).toList();
    }

}
