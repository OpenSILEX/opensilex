//******************************************************************************
//                          GermplasmGetSingleDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.germplasm.api;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.opensilex.core.germplasm.dal.GermplasmModel;

/**
 * DTO representing JSON for searching germplasm or getting them by uri
 *
 * @author Alice Boizet
 */
public class GermplasmGetSingleDTO extends GermplasmGetAllDTO {

    /**
     * Germplasm Variety URI
     */
    protected URI fromVariety;

    /**
     * varietyLabel
     */
    protected String varietyLabel;

    /**
     * Germplasm Accession URI
     */
    protected URI fromAccession;

    /**
     * accessionLabel
     */
    protected String accessionLabel;

    /**
     * institute where the accession has been created
     */
    protected String institute;

    /**
     * Germplasm id (accessionNumber, varietyCode...)
     */
    protected String code;

    /**
     * productionYear
     */
    protected Integer productionYear;

    /**
     * experiment
     */
    protected URI experiment;

    /**
     * comment
     */
    protected String comment;

    protected List<String> synonyms;

    protected Map<String, String> attributes;

    public URI getFromVariety() {
        return fromVariety;
    }

    public void setFromVariety(URI fromVariety) {
        this.fromVariety = fromVariety;
    }

    public String getVarietyLabel() {
        return varietyLabel;
    }

    public void setVarietyLabel(String varietyLabel) {
        this.varietyLabel = varietyLabel;
    }

    public URI getFromAccession() {
        return fromAccession;
    }

    public void setFromAccession(URI fromAccession) {
        this.fromAccession = fromAccession;
    }

    public String getAccessionLabel() {
        return accessionLabel;
    }

    public void setAccessionLabel(String accessionLabel) {
        this.accessionLabel = accessionLabel;
    }

    public String getInstitute() {
        return institute;
    }

    public void setInstitute(String institute) {
        this.institute = institute;
    }

    public Integer getProductionYear() {
        return productionYear;
    }

    public void setProductionYear(Integer productionYear) {
        this.productionYear = productionYear;
    }

    public URI getExperiment() {
        return experiment;
    }

    public void setExperiment(URI experiment) {
        this.experiment = experiment;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(List<String> synonyms) {
        this.synonyms = synonyms;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    /**
     * Convert Germplasm Model into Germplasm DTO
     *
     * @param model Germplasm Model to convert
     * @return Corresponding user DTO
     */
    public static GermplasmGetSingleDTO fromModel(GermplasmModel model) {
        GermplasmGetSingleDTO dto = new GermplasmGetSingleDTO();

        dto.setUri(model.getUri());
        dto.setRdfType(model.getType());
        dto.setTypeLabel(model.getTypeLabel().getDefaultValue());
        dto.setLabel(model.getName());

        if (model.getSpecies() != null) {
            dto.setFromSpecies(model.getSpecies().getUri());
            try {
                dto.setSpeciesLabel(model.getSpecies().getName());
            } catch (Exception e) {
            }
        }

        if (model.getVariety() != null) {
            dto.setFromVariety(model.getVariety().getUri());
            try {
                dto.setVarietyLabel(model.getVariety().getName());
            } catch (Exception e) {
            }

        }

        if (model.getAccession() != null) {
            dto.setFromAccession(model.getAccession().getUri());
            try {
                dto.setAccessionLabel(model.getAccession().getName());
            } catch (Exception e) {
            }

        }

        if (model.getComment() != null) {
            dto.setComment(model.getComment());
        }

        if (model.getInstitute() != null) {
            dto.setInstitute(model.getInstitute());
        }

        if (model.getProductionYear() != null) {
            dto.setProductionYear(model.getProductionYear());
        }

        if (model.getCode() != null) {
            dto.setCode(model.getCode());
        }

        if (model.getAttributes() != null) {
            dto.setAttributes(model.getAttributes());
        }

        if (model.getSynonyms() != null) {
            dto.setSynonyms(model.getSynonyms());
        }

        return dto;
    }

}
