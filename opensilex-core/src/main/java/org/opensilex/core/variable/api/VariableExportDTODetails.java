//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: hamza.ikiou@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variable.api;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.server.rest.validation.ValidURI;


/**
 *
 * @author Hamza IKIOU
 */

@JsonPropertyOrder({
        "uri", "name", "alternativeName", "description", "dataType", "species", "timeInterval", "samplingInterval",
        SKOSReferencesDTO.EXACT_MATCH_JSON_PROPERTY, SKOSReferencesDTO.CLOSE_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.BROAD_MATCH_JSON_PROPERTY, SKOSReferencesDTO.NARROW_MATCH_JSON_PROPERTY,
        "entityURI", "entityName", "entity_narrow_match", "entity_broad_match", "entity_close_match", "entity_exact_match",
        "entityOfInterestURI", "entityOfInterestName", "entityOfInterest_narrow_match", "entityOfInterest_broad_match",
        "entityOfInterest_close_match", "entityOfInterest_exact_match",
        "characteristicURI", "characteristicName", "characteristic_narrow_match", "characteristic_broad_match",
        "characteristic_close_match", "characteristic_exact_match",
        "methodURI", "methodName", "method_narrow_match", "method_broad_match", "method_close_match", "method_exact_match",
        "unitURI", "unitName", "unit_narrow_match", "unit_broad_match", "unit_close_match", "unit_exact_match"
})

public class VariableExportDTODetails extends VariableExportDTOClassic{

    @ValidURI
    @JsonProperty(SKOSReferencesDTO.NARROW_MATCH_JSON_PROPERTY)
    private List<URI> narrowMatch = new ArrayList<>();
    @ValidURI
    @JsonProperty(SKOSReferencesDTO.BROAD_MATCH_JSON_PROPERTY)
    private List<URI> broadMatch = new ArrayList<>();
    @ValidURI
    @JsonProperty(SKOSReferencesDTO.CLOSE_MATCH_JSON_PROPERTY)
    private List<URI> closeMatch = new ArrayList<>();
    @ValidURI
    @JsonProperty(SKOSReferencesDTO.EXACT_MATCH_JSON_PROPERTY)
    private List<URI> exactMatch = new ArrayList<>();
    @ValidURI
    @JsonProperty("entity_narrow_match")
    private List<URI> entityNarrowMatch = new ArrayList<>();
    @ValidURI
    @JsonProperty("entity_broad_match")
    private List<URI> entityBroadMatch = new ArrayList<>();
    @ValidURI
    @JsonProperty("entity_close_match")
    private List<URI> entityCloseMatch = new ArrayList<>();
    @ValidURI
    @JsonProperty("entity_exact_match")
    private List<URI> entityExactMatch = new ArrayList<>();
    @ValidURI
    @JsonProperty("entityOfInterest_narrow_match")
    private List<URI> entityOfInterestNarrowMatch = new ArrayList<>();
    @ValidURI
    @JsonProperty("entityOfInterest_broad_match")
    private List<URI> entityOfInterestBroadMatch = new ArrayList<>();
    @ValidURI
    @JsonProperty("entityOfInterest_close_match")
    private List<URI> entityOfInterestCloseMatch = new ArrayList<>();
    @ValidURI
    @JsonProperty("entityOfInterest_exact_match")
    private List<URI> entityOfInterestExactMatch = new ArrayList<>();
    @ValidURI
    @JsonProperty("characteristic_narrow_match")
    private List<URI> characteristicNarrowMatch = new ArrayList<>();
    @ValidURI
    @JsonProperty("characteristic_broad_match")
    private List<URI> characteristicBroadMatch = new ArrayList<>();
    @ValidURI
    @JsonProperty("characteristic_close_match")
    private List<URI> characteristicCloseMatch = new ArrayList<>();
    @ValidURI
    @JsonProperty("characteristic_exact_match")
    private List<URI> characteristicExactMatch = new ArrayList<>();
    @ValidURI
    @JsonProperty("method_narrow_match")
    private List<URI> methodNarrowMatch = new ArrayList<>();
    @ValidURI
    @JsonProperty("method_broad_match")
    private List<URI> methodBroadMatch = new ArrayList<>();
    @ValidURI
    @JsonProperty("method_close_match")
    private List<URI> methodCloseMatch = new ArrayList<>();
    @ValidURI
    @JsonProperty("method_exact_match")
    private List<URI> methodExactMatch = new ArrayList<>();
    @ValidURI
    @JsonProperty("unit_narrow_match")
    private List<URI> unitNarrowMatch = new ArrayList<>();
    @ValidURI
    @JsonProperty("unit_broad_match")
    private List<URI> unitBroadMatch = new ArrayList<>();
    @ValidURI
    @JsonProperty("unit_close_match")
    private List<URI> unitCloseMatch = new ArrayList<>();
    @ValidURI
    @JsonProperty("unit_exact_match")
    private List<URI> unitExactMatch = new ArrayList<>();


    public VariableExportDTODetails(VariableModel model) {
        super(model);

        this.narrowMatch = model.getNarrowMatch();
        this.broadMatch = model.getBroadMatch();
        this.closeMatch = model.getCloseMatch();
        this.exactMatch = model.getExactMatch();

        this.entityNarrowMatch = model.getEntity().getNarrowMatch();
        this.entityBroadMatch = model.getEntity().getBroadMatch();
        this.entityCloseMatch = model.getEntity().getCloseMatch();
        this.entityExactMatch = model.getEntity().getExactMatch();

        if(model.getEntityOfInterest() != null){
            this.entityOfInterestNarrowMatch = model.getEntityOfInterest().getNarrowMatch();
            this.entityOfInterestBroadMatch = model.getEntityOfInterest().getBroadMatch();
            this.entityOfInterestCloseMatch = model.getEntityOfInterest().getCloseMatch();
            this.entityOfInterestExactMatch = model.getEntityOfInterest().getExactMatch();
        }

        this.characteristicNarrowMatch = model.getCharacteristic().getNarrowMatch();
        this.characteristicBroadMatch = model.getCharacteristic().getBroadMatch();
        this.characteristicCloseMatch = model.getCharacteristic().getCloseMatch();
        this.characteristicExactMatch = model.getCharacteristic().getExactMatch();

        this.methodNarrowMatch = model.getMethod().getNarrowMatch();
        this.methodBroadMatch = model.getMethod().getBroadMatch();
        this.methodCloseMatch = model.getMethod().getCloseMatch();
        this.methodExactMatch = model.getMethod().getExactMatch();

        this.unitNarrowMatch = model.getUnit().getNarrowMatch();
        this.unitBroadMatch = model.getUnit().getBroadMatch();
        this.unitCloseMatch = model.getUnit().getCloseMatch();
        this.unitExactMatch = model.getUnit().getExactMatch();
    }

    public VariableExportDTODetails() {
    }

    public List<URI> getNarrowMatch() {
        return narrowMatch;
    }

    public void setNarrowMatch(List<URI> narrowMatch) {
        this.narrowMatch = narrowMatch;
    }

    public List<URI> getBroadMatch() {
        return broadMatch;
    }

    public void setBroadMatch(List<URI> broadMatch) {
        this.broadMatch = broadMatch;
    }

    public List<URI> getCloseMatch() {
        return closeMatch;
    }

    public void setCloseMatch(List<URI> closeMatch) {
        this.closeMatch = closeMatch;
    }

    public List<URI> getExactMatch() {
        return exactMatch;
    }

    public void setExactMatch(List<URI> exactMatch) {
        this.exactMatch = exactMatch;
    }

    public List<URI> getEntityNarrowMatch() {
        return entityNarrowMatch;
    }

    public void setEntityNarrowMatch(List<URI> entityNarrowMatch) {
        this.entityNarrowMatch = entityNarrowMatch;
    }

    public List<URI> getEntityBroadMatch() {
        return entityBroadMatch;
    }

    public void setEntityBroadMatch(List<URI> entityBroadMatch) {
        this.entityBroadMatch = entityBroadMatch;
    }

    public List<URI> getEntityCloseMatch() {
        return entityCloseMatch;
    }

    public void setEntityCloseMatch(List<URI> entityCloseMatch) {
        this.entityCloseMatch = entityCloseMatch;
    }

    public List<URI> getEntityExactMatch() {
        return entityExactMatch;
    }

    public void setEntityExactMatch(List<URI> entityExactMatch) {
        this.entityExactMatch = entityExactMatch;
    }

    public List<URI> getEntityOfInterestNarrowMatch() {
        return entityOfInterestNarrowMatch;
    }

    public void setEntityOfInterestNarrowMatch(List<URI> entityOfInterestNarrowMatch) {
        this.entityOfInterestNarrowMatch = entityOfInterestNarrowMatch;
    }

    public List<URI> getEntityOfInterestBroadMatch() {
        return entityOfInterestBroadMatch;
    }

    public void setEntityOfInterestBroadMatch(List<URI> entityOfInterestBroadMatch) {
        this.entityOfInterestBroadMatch = entityOfInterestBroadMatch;
    }

    public List<URI> getEntityOfInterestCloseMatch() {
        return entityOfInterestCloseMatch;
    }

    public void setEntityOfInterestCloseMatch(List<URI> entityOfInterestCloseMatch) {
        this.entityOfInterestCloseMatch = entityOfInterestCloseMatch;
    }

    public List<URI> getEntityOfInterestExactMatch() {
        return entityOfInterestExactMatch;
    }

    public void setEntityOfInterestExactMatch(List<URI> entityOfInterestExactMatch) {
        this.entityOfInterestExactMatch = entityOfInterestExactMatch;
    }

    public List<URI> getCharacteristicNarrowMatch() {
        return characteristicNarrowMatch;
    }

    public void setCharacteristicNarrowMatch(List<URI> characteristicNarrowMatch) {
        this.characteristicNarrowMatch = characteristicNarrowMatch;
    }

    public List<URI> getCharacteristicBroadMatch() {
        return characteristicBroadMatch;
    }

    public void setCharacteristicBroadMatch(List<URI> characteristicBroadMatch) {
        this.characteristicBroadMatch = characteristicBroadMatch;
    }

    public List<URI> getCharacteristicCloseMatch() {
        return characteristicCloseMatch;
    }

    public void setCharacteristicCloseMatch(List<URI> characteristicCloseMatch) {
        this.characteristicCloseMatch = characteristicCloseMatch;
    }

    public List<URI> getCharacteristicExactMatch() {
        return characteristicExactMatch;
    }

    public void setCharacteristicExactMatch(List<URI> characteristicExactMatch) {
        this.characteristicExactMatch = characteristicExactMatch;
    }

    public List<URI> getMethodNarrowMatch() {
        return methodNarrowMatch;
    }

    public void setMethodNarrowMatch(List<URI> methodNarrowMatch) {
        this.methodNarrowMatch = methodNarrowMatch;
    }

    public List<URI> getMethodBroadMatch() {
        return methodBroadMatch;
    }

    public void setMethodBroadMatch(List<URI> methodBroadMatch) {
        this.methodBroadMatch = methodBroadMatch;
    }

    public List<URI> getMethodCloseMatch() {
        return methodCloseMatch;
    }

    public void setMethodCloseMatch(List<URI> methodCloseMatch) {
        this.methodCloseMatch = methodCloseMatch;
    }

    public List<URI> getMethodExactMatch() {
        return methodExactMatch;
    }

    public void setMethodExactMatch(List<URI> methodExactMatch) {
        this.methodExactMatch = methodExactMatch;
    }

    public List<URI> getUnitNarrowMatch() {
        return unitNarrowMatch;
    }

    public void setUnitNarrowMatch(List<URI> unitNarrowMatch) {
        this.unitNarrowMatch = unitNarrowMatch;
    }

    public List<URI> getUnitBroadMatch() {
        return unitBroadMatch;
    }

    public void setUnitBroadMatch(List<URI> unitBroadMatch) {
        this.unitBroadMatch = unitBroadMatch;
    }

    public List<URI> getUnitCloseMatch() {
        return unitCloseMatch;
    }

    public void setUnitCloseMatch(List<URI> unitCloseMatch) {
        this.unitCloseMatch = unitCloseMatch;
    }

    public List<URI> getUnitExactMatch() {
        return unitExactMatch;
    }

    public void setUnitExactMatch(List<URI> unitExactMatch) {
        this.unitExactMatch = unitExactMatch;
    }

    public static VariableExportDTODetails fromModel(VariableModel model){
        VariableExportDTODetails dto = new VariableExportDTODetails(model);
        return dto;
    }
}