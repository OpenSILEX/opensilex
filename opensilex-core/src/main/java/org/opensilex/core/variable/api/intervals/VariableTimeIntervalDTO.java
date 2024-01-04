package org.opensilex.core.variable.api.intervals;

public class VariableTimeIntervalDTO {
     private String code;
     private String label;

    public VariableTimeIntervalDTO(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
