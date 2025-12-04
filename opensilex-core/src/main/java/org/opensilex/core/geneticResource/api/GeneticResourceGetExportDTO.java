//******************************************************************************
//                          GeneticResourceGetSingleDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.geneticResource.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.opensilex.core.geneticResource.dal.GeneticResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 * DTO representing JSON for export
 *
 * @author Alice Boizet
 */
@JsonPropertyOrder({"uri", "rdf_type", "rdf_type_name", "name", "synonyms", "code", 
    "production_year", "description", "species", "species_name","variety",
    "variety_name", "accession", "accession_name", "institute", "website","is_public", "groups",
     GeneticResourceGetExportDTO.hasParentGeneticResourceFieldName, GeneticResourceGetExportDTO.hasParentMGeneticResourceFieldName, GeneticResourceGetExportDTO.hasParentFGeneticResourceFieldName})
public class GeneticResourceGetExportDTO extends GeneticResourceGetAllDTO {

    /**
     * GeneticResource Variety URI
     */
    protected URI variety;

    /**
     * varietyName
     */
    @JsonProperty("variety_name")
    protected String varietyName;

    /**
     * GeneticResource Accession URI
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
     * GeneticResource id (accessionNumber, varietyCode...)
     */
    protected String code;

    /**
     * productionYear
     */
    @JsonProperty("production_year")
    protected Integer productionYear;

    public static final String hasParentGeneticResourceFieldName = "has_parent_geneticResource";
    public static final String hasParentMGeneticResourceFieldName = "has_parent_geneticResource_m";
    public static final String hasParentFGeneticResourceFieldName = "has_parent_geneticResource_f";

    @JsonProperty(hasParentGeneticResourceFieldName)
    protected List<GeneticResourceGetAllDTO> hasParentGeneticResource;

    @JsonProperty(hasParentMGeneticResourceFieldName)
    protected List<GeneticResourceGetAllDTO> hasParentGeneticResourceM;

    @JsonProperty(hasParentFGeneticResourceFieldName)
    protected List<GeneticResourceGetAllDTO> hasParentGeneticResourceF;


    @JsonProperty("is_public")
    protected boolean isPublic;

    @JsonProperty("groups")
    protected List<URI> groups = new ArrayList<>();

    public List<URI> setGroups() {
        return groups;
    }

    public void setGroups(List<URI> groups) {
        this.groups = groups;
    }

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

    public List<GeneticResourceGetAllDTO> getHasParentGeneticResource() {
        return hasParentGeneticResource;
    }

    public void setHasParentGeneticResource(List<GeneticResourceGetAllDTO> hasParentGeneticResource) {
        this.hasParentGeneticResource = hasParentGeneticResource;
    }

    public Boolean getIsPublic(){ return isPublic; }
    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public List<GeneticResourceGetAllDTO> getHasParentGeneticResourceM() {
        return hasParentGeneticResourceM;
    }

    public void setHasParentGeneticResourceM(List<GeneticResourceGetAllDTO> hasParentGeneticResourceM) {
        this.hasParentGeneticResourceM = hasParentGeneticResourceM;
    }

    public List<GeneticResourceGetAllDTO> getHasParentGeneticResourceF() {
        return hasParentGeneticResourceF;
    }

    public void setHasParentGeneticResourceF(List<GeneticResourceGetAllDTO> hasParentGeneticResourceF) {
        this.hasParentGeneticResourceF = hasParentGeneticResourceF;
    }

    /**
     * Convert GeneticResource Model into GeneticResource DTO
     *
     * @param model GeneticResource Model to convert
     * @return Corresponding user DTO
     */

    public static GeneticResourceGetExportDTO fromModel(GeneticResourceModel model) {
        GeneticResourceGetExportDTO dto = new GeneticResourceGetExportDTO();

        dto.setUri(model.getUri());
        dto.setRdfType(model.getType());
        dto.setRdfTypeName(model.getTypeLabel().getDefaultValue());
        dto.setName(model.getName());

        dto.setIsPublic(model.getIsPublic());
        dto.setGroups(SPARQLResourceModel.getUriList(model.getGroups()));

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
        if(!CollectionUtils.isEmpty(model.getParentGeneticResources())){
            dto.setHasParentGeneticResource(model.getParentGeneticResources().stream().map((GeneticResourceGetAllDTO::fromModel)).collect(Collectors.toList()));
        }
        if(!CollectionUtils.isEmpty(model.getParentMGeneticResources())){
            dto.setHasParentGeneticResourceM(model.getParentMGeneticResources().stream().map((GeneticResourceGetAllDTO::fromModel)).collect(Collectors.toList()));
        }
        if(!CollectionUtils.isEmpty(model.getParentFGeneticResources())){
            dto.setHasParentGeneticResourceF(model.getParentFGeneticResources().stream().map((GeneticResourceGetAllDTO::fromModel)).collect(Collectors.toList()));
        }

        return dto;
    }

}
