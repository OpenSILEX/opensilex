//******************************************************************************
//                                Accession.java
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
public class Accession {
    private String accessionName;
    private String accessionURI;    
    private String accessionNumber;
    private String varietyLabel;
    private String varietyURI;
    private String speciesURI;
    private String speciesLabel;
    private String genusURI;
    private String genusLabel;
    private ArrayList<String> seedLots;
    private String instituteCode;
    private String instituteName;

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

    public String getGenusURI() {
        return genusURI;
    }

    public void setGenusURI(String genusURI) {
        this.genusURI = genusURI;
    }

    public String getGenusLabel() {
        return genusLabel;
    }

    public void setGenusLabel(String genusLabel) {
        this.genusLabel = genusLabel;
    }

    public ArrayList<String> getSeedLots() {
        return seedLots;
    }

    public void setSeedLots(ArrayList<String> seedLots) {
        this.seedLots = seedLots;
    }

    public String getInstituteCode() {
        return instituteCode;
    }

    public void setInstituteCode(String instituteCode) {
        this.instituteCode = instituteCode;
    }

    public String getInstituteName() {
        return instituteName;
    }

    public void setInstituteName(String instituteName) {
        this.instituteName = instituteName;
    }
    
}
