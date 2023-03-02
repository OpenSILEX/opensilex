package org.opensilex.core.variable.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.List;

public class VariableCopyResponseDTO {

    @JsonProperty("variableUris")
    private List<URI> variableUris;
    @JsonProperty("entityUris")
    private List<URI> entityUris;
    @JsonProperty("characteristicUris")
    private List<URI> characteristicUris;
    @JsonProperty("methodUris")
    private List<URI> methodUris;
    @JsonProperty("unitUris")
    private List<URI> unitUris;
    @JsonProperty("interestEntityUris")
    private List<URI> interestEntityUris;


    public List<URI> getVariableUris() {
        return variableUris;
    }
    public void setVariableUris(List<URI> variableUris) {
        this.variableUris = variableUris;
    }

    public List<URI> getEntityUris() {
        return entityUris;
    }
    public void setEntityUris(List<URI> entityUris) {
        this.entityUris = entityUris;
    }

    public List<URI> getCharacteristicUris() {
        return characteristicUris;
    }
    public void setCharacteristicUris(List<URI> characteristicUris) {
        this.characteristicUris = characteristicUris;
    }

    public List<URI> getMethodUris() {
        return methodUris;
    }
    public void setMethodUris(List<URI> methodUris) {
        this.methodUris = methodUris;
    }

    public List<URI> getUnitUris() {
        return unitUris;
    }
    public void setUnitUris(List<URI> unitUris) {
        this.unitUris = unitUris;
    }

    public List<URI> getInterestEntityUris() {
        return interestEntityUris;
    }
    public void setInterestEntityUris(List<URI> interestEntityUris) {
        this.interestEntityUris = interestEntityUris;
    }

}
