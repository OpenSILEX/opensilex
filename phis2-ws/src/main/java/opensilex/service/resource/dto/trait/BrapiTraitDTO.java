//******************************************************************************
//                                BrapiTraitDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 28 Aug. 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.trait;

import java.util.ArrayList;
import opensilex.service.model.Trait;

/**
 * BrAPI trait DTO.
 * @See https://brapi.docs.apiary.io/#reference/traits
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class BrapiTraitDTO {
    
    private String defaultValue;
    
    /**
     * Comment of the trait.
     * @example one-sided green leaf area per unit ground surface area
     */
    private String description;
    
    /**
     * Label of the trait.
     * @example Leaf_Area_Index
     */
    private String traitName;
    
    private ArrayList<String> observationVariables;
    
    /**
     * URI of the trait.
     * @example http://www.phenome-fppn.fr/platform/id/traits/t001
     */
    private String uri;
    
    private String traitId; 

    public BrapiTraitDTO() {
    }
    
    public BrapiTraitDTO(Trait trait) {
        this.uri = trait.getUri();
        this.traitName = trait.getLabel();
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

    public String getTraitName() {
        return traitName;
    }

    public void setTraitName(String traitName) {
        this.traitName = traitName;
    }

    public ArrayList<String> getObservationVariables() {
        return observationVariables;
    }

    public void setObservationVariables(ArrayList<String> observationVariables) {
        this.observationVariables = observationVariables;
    }

    public String getTraitDbId() {
        return uri;
    }

    public void setTraitDbId(String traitDbId) {
        this.uri = traitDbId;
    }

    public String getTraitId() {
        return traitId;
    }

    public void setTraitId(String traitId) {
        this.traitId = traitId;
    }   
    
}
