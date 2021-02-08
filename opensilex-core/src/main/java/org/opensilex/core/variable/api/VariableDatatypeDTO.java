package org.opensilex.core.variable.api;

import org.apache.jena.datatypes.xsd.XSDDatatype;

import java.net.URI;
import java.net.URISyntaxException;

public class VariableDatatypeDTO {

    public VariableDatatypeDTO(){

    }

    public VariableDatatypeDTO(XSDDatatype datatype, String name) throws URISyntaxException {
        this.uri = new URI(datatype.getURI());
        this.name = name;
    }

    private URI uri;
    private String name;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
