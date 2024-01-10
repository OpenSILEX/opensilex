package org.opensilex.core.variable.api.intervals;

import java.util.Objects;

public class VariableTimeIntervalDTO {
     private String id;
     private String label;

     public VariableTimeIntervalDTO(){}
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariableTimeIntervalDTO that = (VariableTimeIntervalDTO) o;
        return Objects.equals(id, that.id) && Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label);
    }
}
