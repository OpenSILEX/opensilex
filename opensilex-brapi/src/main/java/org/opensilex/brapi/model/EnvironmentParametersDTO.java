package org.opensilex.brapi.model;
/**
 * @see Brapi documentation V2.1 https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI-Core/2.1
 * @author Bernhard Gschloessl
 */
public class EnvironmentParametersDTO {
    private String description; //v2.1 : Human-readable value of the environment parameter (defined above) constant within the experiment
    private String parameterName; //v2.1 : Name of the environment parameter constant within the experiment MIAPPE V1.1 (DM-58) Environment parameter
    private String parameterPUI; //v2.1 : URI pointing to an ontology class for the parameter
    private String unit; //v2.1 : Unit of the value for this parameter
    private String unitPUI; //v2.1 : URI pointing to an ontology class for the unit
    private String value; //v2.1 : Numerical or categorical value MIAPPE V1.1 (DM-59) Environment parameter value
    private String valuePUI; //v2.1 : URI pointing to an ontology class for the parameter value

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public String getParameterPUI() {
        return parameterPUI;
    }

    public void setParameterPUI(String parameterPUI) {
        this.parameterPUI = parameterPUI;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getUnitPUI() {
        return unitPUI;
    }

    public void setUnitPUI(String unitPUI) {
        this.unitPUI = unitPUI;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValuePUI() {
        return valuePUI;
    }

    public void setValuePUI(String valuePUI) {
        this.valuePUI = valuePUI;
    }
}
