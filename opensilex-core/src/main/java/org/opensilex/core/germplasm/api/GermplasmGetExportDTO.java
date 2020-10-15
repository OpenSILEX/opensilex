//******************************************************************************
//                          GermplasmGetSingleDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.germplasm.api;

import java.net.URI;
import java.util.List;
import org.opensilex.core.germplasm.dal.GermplasmModel;

/**
 * DTO representing JSON for export
 *
 * @author Alice Boizet
 */
public class GermplasmGetExportDTO extends GermplasmGetAllDTO {

    /**
     * Germplasm Variety URI
     */
    protected URI variety;

    /**
     * varietyLabel
     */
    protected String varietyLabel;

    /**
     * Germplasm Accession URI
     */
    protected URI accession;

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
     * comment
     */
    protected String comment;

    protected List<String> synonyms;


    public URI getVariety() {
        return variety;
    }

    public void setVariety(URI variety) {
        this.variety = variety;
    }

    public String getVarietyLabel() {
        return varietyLabel;
    }

    public void setVarietyLabel(String varietyLabel) {
        this.varietyLabel = varietyLabel;
    }

    public URI getAccession() {
        return accession;
    }

    public void setAccession(URI accession) {
        this.accession = accession;
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

    /**
     * Convert Germplasm Model into Germplasm DTO
     *
     * @param model Germplasm Model to convert
     * @return Corresponding user DTO
     */
    public static GermplasmGetExportDTO fromModel(GermplasmModel model) {
        GermplasmGetExportDTO dto = new GermplasmGetExportDTO();

        dto.setUri(model.getUri());
        dto.setType(model.getType());
        dto.setTypeLabel(model.getTypeLabel().getDefaultValue());
        dto.setName(model.getName());

        if (model.getSpecies() != null) {
            dto.setSpecies(model.getSpecies().getUri());
            try {
                dto.setSpeciesLabel(model.getSpecies().getName());
            } catch (Exception e) {
            }
        }

        if (model.getVariety() != null) {
            dto.setVariety(model.getVariety().getUri());
            try {
                dto.setVarietyLabel(model.getVariety().getName());
            } catch (Exception e) {
            }
        }

        if (model.getAccession() != null) {
            dto.setAccession(model.getAccession().getUri());
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

        if (model.getSynonyms() != null) {
            dto.setSynonyms(model.getSynonyms());
        }

        return dto;
    }

}
