package org.opensilex.core.variable.api;

import org.apache.jena.datatypes.xsd.XSDDatatype;

import java.net.URI;
import java.net.URISyntaxException;

public class VariableDatatypeDTO {

    public VariableDatatypeDTO(){

    }

    public VariableDatatypeDTO(XSDDatatype datatype, String labelKey) throws URISyntaxException {
        this.uri = new URI(datatype.getURI());
        this.labelKey = labelKey;
    }

    private URI uri;
    private String labelKey;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public void setLabelKey(String labelKey) {
        this.labelKey = labelKey;
    }
}
