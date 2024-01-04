package org.opensilex.core.variable.api.intervals;

public class VariableTimeIntervalDTO {
     private String id;
     private String label;

    public VariableTimeIntervalDTO(String code, String label) {
        this.id = code;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
