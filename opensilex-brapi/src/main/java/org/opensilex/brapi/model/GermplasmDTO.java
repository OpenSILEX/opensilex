//******************************************************************************
//                          GermplasmDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import java.util.List;
import org.opensilex.core.germplasm.dal.GermplasmModel;

/**
 * @see Brapi documentation V1.3
 * https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3
 * @author Alice BOIZET
 */
public class GermplasmDTO {

    protected String accessionNumber;
    protected String acquisitionDate;
    protected String additionalInfo;
    protected String biologicalStatusOfAccessionCode;
    protected String biologicalStatusOfAccessionDescription;
    protected String breedingMethodDbId;
    protected String collection;
    protected String commonCropName;
    protected String countryOfOriginCode;
    protected String defaultDisplayName;
    protected String documentationURL;
    protected List donors;
    protected List externalReferences;
    protected String genus;
    protected String germplasmDbId;
    protected String germplasmName;
    protected List germplasmOrigin;
    protected String germplasmPreprocessing;
    protected String instituteCode;
    protected String instituteName;
    protected String pedigree;
    protected String seedSource;
    protected String seedSourceDescription;
    protected String species;
    protected String speciesAuthority;
    protected List storageTypes;
    protected String subtaxa;
    protected String subtaxaAuthority;
    protected List synonyms;
    protected List taxonIds;

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

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public String getBiologicalStatusOfAccessionCode() {
        return biologicalStatusOfAccessionCode;
    }

    public void setBiologicalStatusOfAccessionCode(String biologicalStatusOfAccessionCode) {
        this.biologicalStatusOfAccessionCode = biologicalStatusOfAccessionCode;
    }

    public String getBiologicalStatusOfAccessionDescription() {
        return biologicalStatusOfAccessionDescription;
    }

    public void setBiologicalStatusOfAccessionDescription(String biologicalStatusOfAccessionDescription) {
        this.biologicalStatusOfAccessionDescription = biologicalStatusOfAccessionDescription;
    }

    public String getBreedingMethodDbId() {
        return breedingMethodDbId;
    }

    public void setBreedingMethodDbId(String breedingMethodDbId) {
        this.breedingMethodDbId = breedingMethodDbId;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public String getCommonCropName() {
        return commonCropName;
    }

    public void setCommonCropName(String commonCropName) {
        this.commonCropName = commonCropName;
    }

    public String getCountryOfOriginCode() {
        return countryOfOriginCode;
    }

    public void setCountryOfOriginCode(String countryOfOriginCode) {
        this.countryOfOriginCode = countryOfOriginCode;
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

    public List getDonors() {
        return donors;
    }

    public void setDonors(List donors) {
        this.donors = donors;
    }

    public List getExternalReferences() {
        return externalReferences;
    }

    public void setExternalReferences(List externalReferences) {
        this.externalReferences = externalReferences;
    }

    public String getGenus() {
        return genus;
    }

    public void setGenus(String genus) {
        this.genus = genus;
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

    public List getGermplasmOrigin() {
        return germplasmOrigin;
    }

    public void setGermplasmOrigin(List germplasmOrigin) {
        this.germplasmOrigin = germplasmOrigin;
    }

    public String getGermplasmPreprocessing() {
        return germplasmPreprocessing;
    }

    public void setGermplasmPreprocessing(String germplasmPreprocessing) {
        this.germplasmPreprocessing = germplasmPreprocessing;
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

    public String getSeedSource() {
        return seedSource;
    }

    public void setSeedSource(String seedSource) {
        this.seedSource = seedSource;
    }

    public String getSeedSourceDescription() {
        return seedSourceDescription;
    }

    public void setSeedSourceDescription(String seedSourceDescription) {
        this.seedSourceDescription = seedSourceDescription;
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

    public List getStorageTypes() {
        return storageTypes;
    }

    public void setStorageTypes(List storageTypes) {
        this.storageTypes = storageTypes;
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

    public List getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(List synonyms) {
        this.synonyms = synonyms;
    }

    public List getTaxonIds() {
        return taxonIds;
    }

    public void setTaxonIds(List taxonIds) {
        this.taxonIds = taxonIds;
    }

    /**
     * Convert Germplasm Model into Germplasm DTO
     *
     * @param model Germplasm Model to convert
     * @return Corresponding user DTO
     */
    public static GermplasmDTO fromModel(GermplasmModel model) {
        GermplasmDTO dto = new GermplasmDTO();
        dto.setGermplasmDbId(model.getUri().toString());

        if (model.getLabel() != null) {
            dto.setGermplasmName(model.getLabel().toString());
        }

        if (model.getCode() != null) {
            dto.setAccessionNumber(model.getCode());
        }

        if (model.getSpecies() != null) {
            try {
                dto.setSpecies(model.getSpecies().getLabel().getDefaultValue());
                dto.setCommonCropName(model.getSpecies().getLabel().getDefaultValue());
            } catch (Exception e) {
                dto.setSpecies(model.getSpecies().getUri().toString());
            }
        }

        if (model.getVariety() != null) {
            try {
                dto.setSubtaxa(model.getVariety().getLabel().getDefaultValue());
            } catch (Exception e) {
                dto.setSubtaxa(model.getVariety().getUri().toString());
            }
        }

        if (model.getInstitute() != null) {
            dto.setInstituteCode(model.getInstitute());
        }

        return dto;
    }
}
