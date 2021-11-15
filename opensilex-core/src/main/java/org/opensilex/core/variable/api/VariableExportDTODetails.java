//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: hamza.ikiou@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variable.api;


import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;
import java.util.List;
import org.opensilex.core.variable.dal.VariableModel;


/**
 *
 * @author Hamza IKIOU
 */

@JsonPropertyOrder({
        "uri", "name", "alternativeName", "description", "dataType", "species", "timeInterval", "samplingInterval",
        "entityURI", "entityName", "entityNarrowMatch", "entityBroadMatch", "entityCloseMatch", "entityExactMatch",
        "entityOfInterestURI", "entityOfInterestName", "entityOfInterestNarrowMatch", "entityOfInterestBroadMatch",
        "entityOfInterestCloseMatch", "entityOfInterestExactMatch",
        "characteristicURI", "characteristicName", "characteristicNarrowMatch", "characteristicBroadMatch",
        "characteristicCloseMatch", "characteristicExactMatch",
        "methodURI", "methodName", "methodNarrowMatch", "methodBroadMatch", "methodCloseMatch", "methodExactMatch",
        "unitURI", "unitName", "unitNarrowMatch", "unitBroadMatch", "unitCloseMatch", "unitExactMatch"
})

public class VariableExportDTODetails extends VariableExportDTOClassic{
        
    private List<URI> entityNarrowMatch;
    
    private List<URI> entityBroadMatch;
    
    private List<URI> entityCloseMatch;
    
    private List<URI> entityExactMatch;
            
    private List<URI> entityOfInterestNarrowMatch;
    
    private List<URI> entityOfInterestBroadMatch;
    
    private List<URI> entityOfInterestCloseMatch;
    
    private List<URI> entityOfInterestExactMatch;
    
    private List<URI> characteristicNarrowMatch;
    
    private List<URI> characteristicBroadMatch;
    
    private List<URI> characteristicCloseMatch;
    
    private List<URI> characteristicExactMatch;
        
    private List<URI> methodNarrowMatch;
    
    private List<URI> methodBroadMatch;
    
    private List<URI> methodCloseMatch;
    
    private List<URI> methodExactMatch; 
    
    private List<URI> unitNarrowMatch;
    
    private List<URI> unitBroadMatch;
    
    private List<URI> unitCloseMatch;
    
    private List<URI> unitExactMatch; 

    
    public VariableExportDTODetails(VariableModel model) {
        super(model);
        
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