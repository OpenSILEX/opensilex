//******************************************************************************
//                                GermplasmDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 24 juin 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.germplasm;

import java.util.ArrayList;
import opensilex.service.model.Germplasm;
import opensilex.service.resource.dto.experiment.ExperimentDTO;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GermplasmPostDTO
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class GermplasmDTO extends AbstractVerifiedClass {
    
    final static Logger LOGGER = LoggerFactory.getLogger(ExperimentDTO.class);
    
    private String accessionNumber;
    private String acquisitionDate;
    private String biologicalStatusOfAccessionCode;
    private String breedingMethodDbId;
    private String commonCropName;
    private String countryOfOrigin;
    private String defaultDisplayName;
    private String documentationURL;
    private ArrayList<String> donors;
    private String genus;
    private String germplasmGenus;
    private String germplasmDbId; //uri
    private String germplasmName;
    private String germplasmPUI;
    private String germplasmSpecies;
    private String instituteCode;
    private String instituteName;
    private String pedigree;
    private String seedsource;    
    private String species;
    private String speciesAuthority;
    private String subtaxa; //variety
    private String subtaxaAuthority;
    private ArrayList<String> synonyms;
    private ArrayList<String> taxonIds;
    private ArrayList<String> typeOfGermplasmStorageCode;

    public GermplasmDTO(Germplasm germplasm) {
        accessionNumber = germplasm.getAccessionNumber();

        //genus = germplasm.getGenus();
        //germplasmGenus = germplasm.getGenus();
        germplasmDbId = germplasm.getVarietyLabel();
        germplasmSpecies = germplasm.getSpeciesLabel();
        species = germplasm.getSpeciesLabel();
        subtaxa = germplasm.getVarietyLabel(); 
        if (germplasm.getAccessionNumber() != null) {
            germplasmName = germplasm.getAccessionNumber();            
        } else if (germplasm.getVarietyLabel() != null) {
            germplasmName = germplasm.getVarietyLabel(); 
        } else {
            germplasmName = germplasm.getSpeciesLabel();
        }
        instituteCode = germplasm.getInstituteCode();
        instituteName = germplasm.getInstituteName();
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getAcquisitionDate() {
        return acquisitionDate;
    }

    public void setAcquisitionDate(String acquisitionDate) {
        this.acquisitionDate = acquisitionDate;
    }

    public String getBiologicalStatusOfAccessionCode() {
        return biologicalStatusOfAccessionCode;
    }

    public void setBiologicalStatusOfAccessionCode(String biologicalStatusOfAccessionCode) {
        this.biologicalStatusOfAccessionCode = biologicalStatusOfAccessionCode;
    }

    public String getBreedingMethodDbId() {
        return breedingMethodDbId;
    }

    public void setBreedingMethodDbId(String breedingMethodDbId) {
        this.breedingMethodDbId = breedingMethodDbId;
    }

    public String getCommonCropName() {
        return commonCropName;
    }

    public void setCommonCropName(String commonCropName) {
        this.commonCropName = commonCropName;
    }

    public String getCountryOfOrigin() {
        return countryOfOrigin;
    }

    public void setCountryOfOrigin(String countryOfOrigin) {
        this.countryOfOrigin = countryOfOrigin;
    }

    public String getDefaultDisplayName() {
        return defaultDisplayName;
    }

    public void setDefaultDisplayName(String defaultDisplayName) {
        this.defaultDisplayName = defaultDisplayName;
    }

    public String getDocumentationURL() {
        return documentationURL;
    }

    public void setDocumentationURL(String documentationURL) {
        this.documentationURL = documentationURL;
    }

    public ArrayList<String> getDonors() {
        return donors;
    }

    public void setDonors(ArrayList<String> donors) {
        this.donors = donors;
    }

    public String getGenus() {
        return genus;
    }

    public void setGenus(String genus) {
        this.genus = genus;
    }

    public String getGermplasmGenus() {
        return germplasmGenus;
    }

    public void setGermplasmGenus(String germplasmGenus) {
        this.germplasmGenus = germplasmGenus;
    }

    public String getGermplasmDbId() {
        return germplasmDbId;
    }

    public void setGermplasmDbId(String germplasmDbId) {
        this.germplasmDbId = germplasmDbId;
    }

    public String getGermplasmName() {
        return germplasmName;
    }

    public void setGermplasmName(String germplasmName) {
        this.germplasmName = germplasmName;
    }

    public String getGermplasmPUI() {
        return germplasmPUI;
    }

    public void setGermplasmPUI(String germplasmPUI) {
        this.germplasmPUI = germplasmPUI;
    }

    public String getGermplasmSpecies() {
        return germplasmSpecies;
    }

    public void setGermplasmSpecies(String germplasmSpecies) {
        this.germplasmSpecies = germplasmSpecies;
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

    public String getPedigree() {
        return pedigree;
    }

    public void setPedigree(String pedigree) {
        this.pedigree = pedigree;
    }

    public String getSeedsource() {
        return seedsource;
    }

    public void setSeedsource(String seedsource) {
        this.seedsource = seedsource;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getSpeciesAuthority() {
        return speciesAuthority;
    }

    public void setSpeciesAuthority(String speciesAuthority) {
        this.speciesAuthority = speciesAuthority;
    }

    public String getSubtaxa() {
        return subtaxa;
    }

    public void setSubtaxa(String subtaxa) {
        this.subtaxa = subtaxa;
    }

    public String getSubtaxaAuthority() {
        return subtaxaAuthority;
    }

    public void setSubtaxaAuthority(String subtaxaAuthority) {
        this.subtaxaAuthority = subtaxaAuthority;
    }

    public ArrayList<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(ArrayList<String> synonyms) {
        this.synonyms = synonyms;
    }

    public ArrayList<String> getTaxonIds() {
        return taxonIds;
    }

    public void setTaxonIds(ArrayList<String> taxonIds) {
        this.taxonIds = taxonIds;
    }

    public ArrayList<String> getTypeOfGermplasmStorageCode() {
        return typeOfGermplasmStorageCode;
    }

    public void setTypeOfGermplasmStorageCode(ArrayList<String> typeOfGermplasmStorageCode) {
        this.typeOfGermplasmStorageCode = typeOfGermplasmStorageCode;
    }

    @Override
    public Object createObjectFromDTO() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
