//******************************************************************************
//                          Faidarev1GeneticResourceDTO.java
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
public class Faidarev1GeneticResourceDTO {

    protected String accessionNumber;
    protected String acquisitionDate;
    protected Integer biologicalStatusOfAccessionCode;
    protected String commonCropName;
    protected String countryOfOriginCode;
    protected String defaultDisplayName;
    protected String documentationURL;
    protected List<Object> donors; // type Donor has no equivalent in OpenSILEX
    protected String genus;
    protected String geneticResourceDbId;
    protected String geneticResourceName;
    protected String geneticResourcePUI;
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
    protected List<String> typeOfGeneticResourceStorageCode;
    @JsonProperty("studyDbIb") // error on faidare's side
    protected List<String> studyDbId;

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public Faidarev1GeneticResourceDTO setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
        return this;
    }

    public String getAcquisitionDate() {
        return acquisitionDate;
    }

    public Faidarev1GeneticResourceDTO setAcquisitionDate(String acquisitionDate) {
        this.acquisitionDate = acquisitionDate;
        return this;
    }

    public Integer getBiologicalStatusOfAccessionCode() {
        return biologicalStatusOfAccessionCode;
    }

    public Faidarev1GeneticResourceDTO setBiologicalStatusOfAccessionCode(Integer biologicalStatusOfAccessionCode) {
        this.biologicalStatusOfAccessionCode = biologicalStatusOfAccessionCode;
        return this;
    }

    public String getCommonCropName() {
        return commonCropName;
    }

    public Faidarev1GeneticResourceDTO setCommonCropName(String commonCropName) {
        this.commonCropName = commonCropName;
        return this;
    }

    public String getCountryOfOriginCode() {
        return countryOfOriginCode;
    }

    public Faidarev1GeneticResourceDTO setCountryOfOriginCode(String countryOfOriginCode) {
        this.countryOfOriginCode = countryOfOriginCode;
        return this;
    }

    public String getDefaultDisplayName() {
        return defaultDisplayName;
    }

    public Faidarev1GeneticResourceDTO setDefaultDisplayName(String defaultDisplayName) {
        this.defaultDisplayName = defaultDisplayName;
        return this;
    }

    public String getDocumentationURL() {
        return documentationURL;
    }

    public Faidarev1GeneticResourceDTO setDocumentationURL(String documentationURL) {
        this.documentationURL = documentationURL;
        return this;
    }

    public List<Object> getDonors() {
        return donors;
    }

    public Faidarev1GeneticResourceDTO setDonors(List<Object> donors) {
        this.donors = donors;
        return this;
    }

    public String getGeneticResourceDbId() {
        return geneticResourceDbId;
    }

    public Faidarev1GeneticResourceDTO setGeneticResourceDbId(String geneticResourceDbId) {
        this.geneticResourceDbId = geneticResourceDbId;
        return this;
    }

    public String getGeneticResourceName() {
        return geneticResourceName;
    }

    public Faidarev1GeneticResourceDTO setGeneticResourceName(String geneticResourceName) {
        this.geneticResourceName = geneticResourceName;
        return this;
    }

    public String getGeneticResourcePUI() {
        return geneticResourcePUI;
    }

    public Faidarev1GeneticResourceDTO setGeneticResourcePUI(String geneticResourcePUI) {
        this.geneticResourcePUI = geneticResourcePUI;
        return this;
    }

    public String getInstituteCode() {
        return instituteCode;
    }

    public Faidarev1GeneticResourceDTO setInstituteCode(String instituteCode) {
        this.instituteCode = instituteCode;
        return this;
    }

    public String getInstituteName() {
        return instituteName;
    }

    public Faidarev1GeneticResourceDTO setInstituteName(String instituteName) {
        this.instituteName = instituteName;
        return this;
    }

    public String getPedigree() {
        return pedigree;
    }

    public Faidarev1GeneticResourceDTO setPedigree(String pedigree) {
        this.pedigree = pedigree;
        return this;
    }

    public String getSeedSource() {
        return seedSource;
    }

    public Faidarev1GeneticResourceDTO setSeedSource(String seedSource) {
        this.seedSource = seedSource;
        return this;
    }

    public String getSpeciesAuthority() {
        return speciesAuthority;
    }

    public Faidarev1GeneticResourceDTO setSpeciesAuthority(String speciesAuthority) {
        this.speciesAuthority = speciesAuthority;
        return this;
    }

    public String getSubtaxa() {
        return subtaxa;
    }

    public Faidarev1GeneticResourceDTO setSubtaxa(String subtaxa) {
        this.subtaxa = subtaxa;
        return this;
    }

    public String getSubtaxaAuthority() {
        return subtaxaAuthority;
    }

    public Faidarev1GeneticResourceDTO setSubtaxaAuthority(String subtaxaAuthority) {
        this.subtaxaAuthority = subtaxaAuthority;
        return this;
    }

    public List<String> getSynonyms() {
        return synonyms;
    }

    public Faidarev1GeneticResourceDTO setSynonyms(List<String> synonyms) {
        this.synonyms = synonyms;
        return this;
    }

    public List<Object> getTaxonIds() {
        return taxonIds;
    }

    public Faidarev1GeneticResourceDTO setTaxonIds(List<Object> taxonIds) {
        this.taxonIds = taxonIds;
        return this;
    }

    public List<String> getTypeOfGeneticResourceStorageCode() {
        return typeOfGeneticResourceStorageCode;
    }

    public Faidarev1GeneticResourceDTO setTypeOfGeneticResourceStorageCode(List<String> typeOfGeneticResourceStorageCode) {
        this.typeOfGeneticResourceStorageCode = typeOfGeneticResourceStorageCode;
        return this;
    }

    public List<String> getStudyDbId() {
        return studyDbId;
    }

    public Faidarev1GeneticResourceDTO setStudyDbId(List<String> studyDbId) {
        this.studyDbId = studyDbId;
        return this;
    }

    public String getGenus() {
        return genus;
    }

    public Faidarev1GeneticResourceDTO setGenus(String genus) {
        this.genus = genus;
        return this;
    }

    public String getSpecies() {
        return species;
    }

    public Faidarev1GeneticResourceDTO setSpecies(String species) {
        this.species = species;
        return this;
    }
}
