//******************************************************************************
//                          GermplasmGetSingleDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.germplasm.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.opensilex.core.germplasm.dal.GermplasmModel;

/**
 * DTO representing JSON for export
 *
 * @author Alice Boizet
 */
@JsonPropertyOrder({"uri", "rdf_type", "rdf_type_name", "name", "synonyms", "code", 
    "production_year", "description", "species", "species_name","variety", 
    "variety_name", "accession", "accession_name", "institute", "website",
    GermplasmGetExportDTO.hasParentGermplasmFieldName, GermplasmGetExportDTO.hasParentMGermplasmFieldName, GermplasmGetExportDTO.hasParentFGermplasmFieldName})
public class GermplasmGetExportDTO extends GermplasmGetAllDTO {

    /**
     * Germplasm Variety URI
     */
    protected URI variety;

    /**
     * varietyName
     */
    @JsonProperty("variety_name")
    protected String varietyName;

    /**
     * Germplasm Accession URI
     */
    protected URI accession;

    /**
     * accessionName
     */
    @JsonProperty("accession_name")
    protected String accessionName;

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
    @JsonProperty("production_year")
    protected Integer productionYear;

    public static final String hasParentGermplasmFieldName = "has_parent_germplasm";
    public static final String hasParentMGermplasmFieldName = "has_parent_germplasm_m";
    public static final String hasParentFGermplasmFieldName = "has_parent_germplasm_f";

    @JsonProperty(hasParentGermplasmFieldName)
    protected List<GermplasmGetAllDTO> hasParentGermplasm;

    @JsonProperty(hasParentMGermplasmFieldName)
    protected List<GermplasmGetAllDTO> hasParentGermplasmM;

    @JsonProperty(hasParentFGermplasmFieldName)
    protected List<GermplasmGetAllDTO> hasParentGermplasmF;

    /**
     * description
     */
    protected String description;

    protected List<String> synonyms;
    
    protected URI website;

    public URI getVariety() {
        return variety;
    }

    public void setVariety(URI variety) {
        this.variety = variety;
    }

    public String getVarietyName() {
        return varietyName;
    }

    public void setVarietyName(String varietyName) {
        this.varietyName = varietyName;
    }

    public URI getAccession() {
        return accession;
    }

    public void setAccession(URI accession) {
        this.accession = accession;
    }

    public String getAccessionName() {
        return accessionName;
    }

    public void setAccessionName(String accessionName) {
        this.accessionName = accessionName;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public URI getWebsite() {
        return website;
    }

    public void setWebsite(URI website) {
        this.website = website;
    }

    public List<GermplasmGetAllDTO> getHasParentGermplasm() {
        return hasParentGermplasm;
    }

    public void setHasParentGermplasm(List<GermplasmGetAllDTO> hasParentGermplasm) {
        this.hasParentGermplasm = hasParentGermplasm;
    }

    public List<GermplasmGetAllDTO> getHasParentGermplasmM() {
        return hasParentGermplasmM;
    }

    public void setHasParentGermplasmM(List<GermplasmGetAllDTO> hasParentGermplasmM) {
        this.hasParentGermplasmM = hasParentGermplasmM;
    }

    public List<GermplasmGetAllDTO> getHasParentGermplasmF() {
        return hasParentGermplasmF;
    }

    public void setHasParentGermplasmF(List<GermplasmGetAllDTO> hasParentGermplasmF) {
        this.hasParentGermplasmF = hasParentGermplasmF;
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
        dto.setRdfType(model.getType());
        dto.setRdfTypeName(model.getTypeLabel().getDefaultValue());
        dto.setName(model.getName());

        if (model.getSpecies() != null) {
            dto.setSpecies(model.getSpecies().getUri());
            try {
                dto.setSpeciesName(model.getSpecies().getName());
            } catch (Exception e) {
            }
        }

        if (model.getVariety() != null) {
            dto.setVariety(model.getVariety().getUri());
            try {
                dto.setVarietyName(model.getVariety().getName());
            } catch (Exception e) {
            }
        }

        if (model.getAccession() != null) {
            dto.setAccession(model.getAccession().getUri());
            try {
                dto.setAccessionName(model.getAccession().getName());
            } catch (Exception e) {
            }
        }

        if (model.getComment() != null) {
            dto.setDescription(model.getComment());
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
        
        if (model.getWebsite() != null) {
            dto.setWebsite(model.getWebsite());
        }
        if(!CollectionUtils.isEmpty(model.getParentGermplasms())){
            dto.setHasParentGermplasm(model.getParentGermplasms().stream().map((GermplasmGetAllDTO::fromModel)).collect(Collectors.toList()));
        }
        if(!CollectionUtils.isEmpty(model.getParentMGermplasms())){
            dto.setHasParentGermplasmM(model.getParentMGermplasms().stream().map((GermplasmGetAllDTO::fromModel)).collect(Collectors.toList()));
        }
        if(!CollectionUtils.isEmpty(model.getParentFGermplasms())){
            dto.setHasParentGermplasmF(model.getParentFGermplasms().stream().map((GermplasmGetAllDTO::fromModel)).collect(Collectors.toList()));
        }

        return dto;
    }

}
