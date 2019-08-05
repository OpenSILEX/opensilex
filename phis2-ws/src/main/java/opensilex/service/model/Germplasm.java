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
    private String germplasmURI;
    private String accessionNumber;
    private String accessionURI;
    private String varietyLabel;
    private String varietyURI;
    private String speciesURI;
    private String speciesLabel;

    
    public String getGermplasmURI() {
        return germplasmURI;
    }

//    //private String acquisitionDate;
//    private String biologicalStatusOfAccessionCode;
//    //private String breedingMethodDbId;
//    private String commonCropName;
//    //private String countryOfOrigin;
//    //private String defaultDisplayName;
//    private String documentationURL;
//    //private ArrayList<String> donors;
//    private String genus;
//    private String germplasmDbId;  //uri
//    private String germplasmName;
//    //private String germplasmPUI;
//    private String instituteCode;
//    private String instituteName;
//    //private String pedigree;
//    //private String seedsource;    
//    private String species;
//    //private String speciesAuthority;
//    
//    //private String subtaxaAuthority;
//    private ArrayList<String> synonyms;
//    //private ArrayList<String> taxonIds;
//    //private ArrayList<String> typeOfGermplasmStorageCode;
    
    public void setGermplasmURI(String germplasmURI) {
        this.germplasmURI = germplasmURI;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getAccessionURI() {
        return accessionURI;
    }

    public void setAccessionURI(String accessionURI) {
        this.accessionURI = accessionURI;
    }

    public String getVarietyLabel() {
        return varietyLabel;
    }

    public void setVarietyLabel(String varietyLabel) {
        this.varietyLabel = varietyLabel;
    }

    public String getVarietyURI() {
        return varietyURI;
    }

    public void setVarietyURI(String varietyURI) {
        this.varietyURI = varietyURI;
    }

    public String getSpeciesURI() {
        return speciesURI;
    }

    public void setSpeciesURI(String speciesURI) {
        this.speciesURI = speciesURI;
    }

    public String getSpeciesLabel() {
        return speciesLabel;
    }

    public void setSpeciesLabel(String speciesLabel) {
        this.speciesLabel = speciesLabel;
    }   
    
}
