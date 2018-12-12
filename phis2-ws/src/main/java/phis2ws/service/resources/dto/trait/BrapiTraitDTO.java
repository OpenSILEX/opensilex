//******************************************************************************
//                                       BrapiTraitDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 28 august 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.trait;

import java.util.ArrayList;
import phis2ws.service.view.model.phis.Trait;

/**
 * Represents a trait according to brapi specifications
 * @See https://brapi.docs.apiary.io/#reference/traits
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class BrapiTraitDTO {
    private String defaultValue;
    //The comment of the trait in the triplestore e.g. "one-sided green leaf area per unit ground surface area"
    private String description;
    //The label of the trait in the triplestore e.g. Leaf_Area_Index
    private String name;
    private ArrayList<String> observationVariables;
    //The id of the trait in the triplestore e.g. http://www.phenome-fppn.fr/platform/id/traits/t001
    private String traitDbId;
    private String traitId; 

    public BrapiTraitDTO() {
    }
    
    public BrapiTraitDTO(Trait trait) {
        this.traitDbId = trait.getUri();
        this.name = trait.getLabel();
        this.description = trait.getComment();
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
