//******************************************************************************
//                                       BrapiTrait.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 28 août 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.model.phis;

import java.util.ArrayList;

/**
 *
 * @author boizetal
 */
public class BrapiTrait {
    private String defaultValue;
    private String description;
    private String name;
    private ArrayList<String> observationVariables;
    private String traitDbId;
    private String traitId; 

    public BrapiTrait() {
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<String> getObservationVariables() {
        return observationVariables;
    }

    public void setObservationVariables(ArrayList<String> observationVariables) {
        this.observationVariables = observationVariables;
    }

    public String getTraitDbId() {
        return traitDbId;
    }

    public void setTraitDbId(String traitDbId) {
        this.traitDbId = traitDbId;
    }

    public String getTraitId() {
        return traitId;
    }

    public void setTraitId(String traitId) {
        this.traitId = traitId;
    }   
    
}
