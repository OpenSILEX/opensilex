//******************************************************************************
//                                geneticInformation.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 6 sept. 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

/**
 * genetic Information (returned in scientific Object)
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class GeneticInformation {  
    private String accessionNumber;
    private String varietyLabel;
    private String speciesLabel;
    private String genusLabel;
    private String plantMaterialLot;

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getVarietyLabel() {
        return varietyLabel;
    }

    public void setVarietyLabel(String varietyLabel) {
        this.varietyLabel = varietyLabel;
    }

    public String getSpeciesLabel() {
        return speciesLabel;
    }

    public void setSpeciesLabel(String speciesLabel) {
        this.speciesLabel = speciesLabel;
    }

    public String getGenusLabel() {
        return genusLabel;
    }

    public void setGenusLabel(String genusLabel) {
        this.genusLabel = genusLabel;
    }

    public String getPlantMaterialLot() {
        return plantMaterialLot;
    }

    public void setPlantMaterialLot(String plantMaterialLot) {
        this.plantMaterialLot = plantMaterialLot;
    }    
    
}

