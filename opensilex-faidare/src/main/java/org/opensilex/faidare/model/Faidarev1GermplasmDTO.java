//******************************************************************************
//                          Faidarev1GermplasmDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Faidarev1ContactDTO: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.faidare.model;

import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.germplasm.api.GermplasmSearchFilter;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.security.account.dal.AccountModel;

import java.util.List;
import java.util.stream.Collectors;

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
    protected String germplasmDbId;
    protected String germplasmName;
    protected String germplasmPUI;
    protected String instituteCode;
    protected String instituteName;
    protected String pedigree;
    protected String seedSource;
    protected String speciesAuthority;
    protected String subtaxa;
    protected String subtaxaAuthority;
    protected List<String> synonyms;
    protected List<Object> taxonIds; // type TaxonId has no equivalent in OpenSILEX
    protected String sourceName;
    protected String taxonId;
    protected List<String> typeOfGermplasmStorageCode;
    protected List<String> studyDbId;


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

    public Integer getBiologicalStatusOfAccessionCode() {
        return biologicalStatusOfAccessionCode;
    }

    public void setBiologicalStatusOfAccessionCode(Integer biologicalStatusOfAccessionCode) {
        this.biologicalStatusOfAccessionCode = biologicalStatusOfAccessionCode;
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

    public List<Object> getDonors() {
        return donors;
    }

    public void setDonors(List<Object> donors) {
        this.donors = donors;
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

    public List<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(List<String> synonyms) {
        this.synonyms = synonyms;
    }

    public List<Object> getTaxonIds() {
        return taxonIds;
    }

    public void setTaxonIds(List<Object> taxonIds) {
        this.taxonIds = taxonIds;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getTaxonId() {
        return taxonId;
    }

    public void setTaxonId(String taxonId) {
        this.taxonId = taxonId;
    }

    public List<String> getTypeOfGermplasmStorageCode() {
        return typeOfGermplasmStorageCode;
    }

    public void setTypeOfGermplasmStorageCode(List<String> typeOfGermplasmStorageCode) {
        this.typeOfGermplasmStorageCode = typeOfGermplasmStorageCode;
    }

    public List<String> getStudyDbId() {
        return studyDbId;
    }

    public void setStudyDbId(List<String> studyDbId) {
        this.studyDbId = studyDbId;
    }

    /**
     * Convert Germplasm Model into Germplasm DTO
     *
     * @param model Germplasm Model to convert
     * @return Corresponding user DTO
     */
    public static Faidarev1GermplasmDTO fromModel(GermplasmModel model, GermplasmDAO germplasmDAO, AccountModel accountModel) throws Exception {
        Faidarev1GermplasmDTO dto = new Faidarev1GermplasmDTO();
        dto.setGermplasmDbId(model.getUri().toString());
        dto.setGermplasmPUI(model.getUri().toString());

        if (model.getLabel() != null) {
            dto.setGermplasmName(model.getLabel().toString());
            dto.setDefaultDisplayName(model.getLabel().toString());
        }

        if (model.getCode() != null) {
            dto.setAccessionNumber(model.getCode());
        }

        if (model.getVariety() != null) {
            try {
                dto.setSubtaxa("var. " + model.getVariety().getLabel().getDefaultValue()); //TODO : change this to latin label when Multilabels are added because the name isn't necessarily in latin
            } catch (Exception e) {
                dto.setSubtaxa(model.getVariety().getUri().toString());
            }
        }

        if (model.getInstitute() != null) {
            dto.setInstituteCode(model.getInstitute());
        }

        if (model.getWebsite() != null) {
            dto.setDocumentationURL(model.getWebsite().toString());
        }

        if (model.getSynonyms() != null) {
            dto.setSynonyms(model.getSynonyms());
        }

        dto.setStudyDbId(
                germplasmDAO.getExpFromGermplasm(accountModel, model.getUri(), null, null, null, null)
                        .getList()
                        .stream()
                        .map(experimentModel -> experimentModel.getUri().toString())
                        .collect(Collectors.toList())
        );

        return dto;
    }
}
