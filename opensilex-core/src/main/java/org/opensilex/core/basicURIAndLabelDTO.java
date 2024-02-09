package org.opensilex.core;

import java.util.Objects;

public class basicURIAndLabelDTO {
     private String uri;
     private String label;

     public basicURIAndLabelDTO(){}
    public basicURIAndLabelDTO(String code, String label) {
        this.uri = code;
        this.label = label;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        basicURIAndLabelDTO that = (basicURIAndLabelDTO) o;
        return Objects.equals(uri, that.uri) && Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, label);
    }
}
