//******************************************************************************
//                                GermplasmPostDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 24 juin 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.germplasm;

import java.util.ArrayList;
import opensilex.service.model.Germplasm;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;

/**
 * GermplasmPostDTO
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class GermplasmPostDTO extends AbstractVerifiedClass {
    /*voir https://www.genesys-pgr.org/fr/doc/0/basics*/
    
    private String accessionNumber;
//    private String acquisitionDate;
    private String biologicalStatusOfAccessionCode;
//    private String breedingMethodDbId;
    private String commonCropName;
//    private String countryOfOrigin;
//    private String defaultDisplayName;
    private String documentationURL; //webSite page to the accession information
//    private String donors;
    private String genus;
    private String germplasmName;
//    private String germplasmPUI;
    private String instituteCode;
    private String instituteName;
//    private String pedigree;
//    private String seedsource;    
    private String species;
//    private String speciesAuthority;
    private String subtaxa; //variety
//    private String subtaxaAuthority;
    private ArrayList<String> synonyms;
//    private ArrayList<String> taxonIds;
//    private ArrayList<String> typeOfGermplasmStorageCode; 


    @Override
    public Germplasm createObjectFromDTO() throws Exception {
        Germplasm germplasm = new Germplasm();
        germplasm.setAccessionNumber(accessionNumber);
        germplasm.setBiologicalStatusOfAccessionCode(biologicalStatusOfAccessionCode);
        germplasm.setDocumentationURL(documentationURL);
        germplasm.setGenus(genus);
        germplasm.setGermplasmName(germplasmName);
        germplasm.setInstituteCode(instituteCode);
        germplasm.setInstituteName(instituteName);
        germplasm.setSpecies(species);
        germplasm.setSubtaxa(subtaxa);
        germplasm.setSynonyms(synonyms);
        
        return germplasm;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getBiologicalStatusOfAccessionCode() {
        return biologicalStatusOfAccessionCode;
    }

    public void setBiologicalStatusOfAccessionCode(String biologicalStatusOfAccessionCode) {
        this.biologicalStatusOfAccessionCode = biologicalStatusOfAccessionCode;
    }

    public String getCommonCropName() {
        return commonCropName;
    }

    public void setCommonCropName(String commonCropName) {
        this.commonCropName = commonCropName;
    }

    public String getDocumentationURL() {
        return documentationURL;
    }

    public void setDocumentationURL(String documentationURL) {
        this.documentationURL = documentationURL;
    }

    public String getGenus() {
        return genus;
    }

    public void setGenus(String genus) {
        this.genus = genus;
    }

    public String getGermplasmName() {
        return germplasmName;
    }

    public void setGermplasmName(String germplasmName) {
        this.germplasmName = germplasmName;
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

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getSubtaxa() {
        return subtaxa;
    }

    public void setSubtaxa(String subtaxa) {
        this.subtaxa = subtaxa;
    }

    public ArrayList<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(ArrayList<String> synonyms) {
        this.synonyms = synonyms;
    }

}
