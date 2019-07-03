//******************************************************************************
//                                Germplasm.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 1 juil. 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import java.util.ArrayList;

/**
 * Model of the germplasms
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class Germplasm {
    private String uri;
    private String label;
    private String species;
    private String variety;
    private String accessionName;
    private String accessionNumber;
    private ArrayList<String> plantMaterialLots;
    private String institute;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
    
    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getVariety() {
        return variety;
    }

    public void setVariety(String variety) {
        this.variety = variety;
    }

    public String getAccessionName() {
        return accessionName;
    }

    public void setAccessionName(String accessionName) {
        this.accessionName = accessionName;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public ArrayList<String> getPlantMaterialLots() {
        return plantMaterialLots;
    }

    public void setPlantMaterialLots(ArrayList<String> plantMaterialLots) {
        this.plantMaterialLots = plantMaterialLots;
    }

    public String getInstitute() {
        return institute;
    }

    public void setInstitute(String institute) {
        this.institute = institute;
    }    
    
}
