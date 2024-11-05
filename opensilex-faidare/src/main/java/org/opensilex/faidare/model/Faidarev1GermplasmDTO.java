//******************************************************************************
//                          Faidarev1GermplasmDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Faidarev1ContactDTO: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.faidare.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author Gabriel Besombes
 */
public class Faidarev1GermplasmDTO {

    protected String accessionNumber;
    protected String acquisitionDate;
    protected Integer biologicalStatusOfAccessionCode;
    protected String commonCropName;
    protected String countryOfOriginCode;
    protected String defaultDisplayName;
    protected String documentationURL;
    protected List<Object> donors; // type Donor has no equivalent in OpenSILEX
    protected String genus;
    protected String germplasmDbId;
    protected String germplasmName;
    protected String germplasmPUI;
    protected String instituteCode;
    protected String instituteName;
    protected String pedigree;
    protected String seedSource;
    protected String species;
    protected String speciesAuthority;
    protected String subtaxa;
    protected String subtaxaAuthority;
    protected List<String> synonyms;
    protected List<Object> taxonIds; // type TaxonId has no equivalent in OpenSILEX
    protected List<String> typeOfGermplasmStorageCode;
    @JsonProperty("studyDbIb") // error on faidare's side
    protected List<String> studyDbId;

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public Faidarev1GermplasmDTO setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
        return this;
    }

    public String getAcquisitionDate() {
        return acquisitionDate;
    }

    public Faidarev1GermplasmDTO setAcquisitionDate(String acquisitionDate) {
        this.acquisitionDate = acquisitionDate;
        return this;
    }

    public Integer getBiologicalStatusOfAccessionCode() {
        return biologicalStatusOfAccessionCode;
    }

    public Faidarev1GermplasmDTO setBiologicalStatusOfAccessionCode(Integer biologicalStatusOfAccessionCode) {
        this.biologicalStatusOfAccessionCode = biologicalStatusOfAccessionCode;
        return this;
    }

    public String getCommonCropName() {
        return commonCropName;
    }

    public Faidarev1GermplasmDTO setCommonCropName(String commonCropName) {
        this.commonCropName = commonCropName;
        return this;
    }

    public String getCountryOfOriginCode() {
        return countryOfOriginCode;
    }

    public Faidarev1GermplasmDTO setCountryOfOriginCode(String countryOfOriginCode) {
        this.countryOfOriginCode = countryOfOriginCode;
        return this;
    }

    public String getDefaultDisplayName() {
        return defaultDisplayName;
    }

    public Faidarev1GermplasmDTO setDefaultDisplayName(String defaultDisplayName) {
        this.defaultDisplayName = defaultDisplayName;
        return this;
    }

    public String getDocumentationURL() {
        return documentationURL;
    }

    public Faidarev1GermplasmDTO setDocumentationURL(String documentationURL) {
        this.documentationURL = documentationURL;
        return this;
    }

    public List<Object> getDonors() {
        return donors;
    }

    public Faidarev1GermplasmDTO setDonors(List<Object> donors) {
        this.donors = donors;
        return this;
    }

    public String getGermplasmDbId() {
        return germplasmDbId;
    }

    public Faidarev1GermplasmDTO setGermplasmDbId(String germplasmDbId) {
        this.germplasmDbId = germplasmDbId;
        return this;
    }

    public String getGermplasmName() {
        return germplasmName;
    }

    public Faidarev1GermplasmDTO setGermplasmName(String germplasmName) {
        this.germplasmName = germplasmName;
        return this;
    }

    public String getGermplasmPUI() {
        return germplasmPUI;
    }

    public Faidarev1GermplasmDTO setGermplasmPUI(String germplasmPUI) {
        this.germplasmPUI = germplasmPUI;
        return this;
    }

    public String getInstituteCode() {
        return instituteCode;
    }

    public Faidarev1GermplasmDTO setInstituteCode(String instituteCode) {
        this.instituteCode = instituteCode;
        return this;
    }

    public String getInstituteName() {
        return instituteName;
    }

    public Faidarev1GermplasmDTO setInstituteName(String instituteName) {
        this.instituteName = instituteName;
        return this;
    }

    public String getPedigree() {
        return pedigree;
    }

    public Faidarev1GermplasmDTO setPedigree(String pedigree) {
        this.pedigree = pedigree;
        return this;
    }

    public String getSeedSource() {
        return seedSource;
    }

    public Faidarev1GermplasmDTO setSeedSource(String seedSource) {
        this.seedSource = seedSource;
        return this;
    }

    public String getSpeciesAuthority() {
        return speciesAuthority;
    }

    public Faidarev1GermplasmDTO setSpeciesAuthority(String speciesAuthority) {
        this.speciesAuthority = speciesAuthority;
        return this;
    }

    public String getSubtaxa() {
        return subtaxa;
    }

    public Faidarev1GermplasmDTO setSubtaxa(String subtaxa) {
        this.subtaxa = subtaxa;
        return this;
    }

    public String getSubtaxaAuthority() {
        return subtaxaAuthority;
    }

    public Faidarev1GermplasmDTO setSubtaxaAuthority(String subtaxaAuthority) {
        this.subtaxaAuthority = subtaxaAuthority;
        return this;
    }

    public List<String> getSynonyms() {
        return synonyms;
    }

    public Faidarev1GermplasmDTO setSynonyms(List<String> synonyms) {
        this.synonyms = synonyms;
        return this;
    }

    public List<Object> getTaxonIds() {
        return taxonIds;
    }

    public Faidarev1GermplasmDTO setTaxonIds(List<Object> taxonIds) {
        this.taxonIds = taxonIds;
        return this;
    }

    public List<String> getTypeOfGermplasmStorageCode() {
        return typeOfGermplasmStorageCode;
    }

    public Faidarev1GermplasmDTO setTypeOfGermplasmStorageCode(List<String> typeOfGermplasmStorageCode) {
        this.typeOfGermplasmStorageCode = typeOfGermplasmStorageCode;
        return this;
    }

    public List<String> getStudyDbId() {
        return studyDbId;
    }

    public Faidarev1GermplasmDTO setStudyDbId(List<String> studyDbId) {
        this.studyDbId = studyDbId;
        return this;
    }

    public String getGenus() {
        return genus;
    }

    public Faidarev1GermplasmDTO setGenus(String genus) {
        this.genus = genus;
        return this;
    }

    public String getSpecies() {
        return species;
    }

    public Faidarev1GermplasmDTO setSpecies(String species) {
        this.species = species;
        return this;
    }
}
