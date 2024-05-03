//******************************************************************************
//                          GermplasmGetSingleDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.germplasm.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.opensilex.core.germplasm.dal.GermplasmModel;

/**
 * DTO representing JSON for searching germplasm or getting them by uri
 *
 * @author Alice Boizet
 */
@JsonPropertyOrder({"uri", "publisher", "publication_date", "last_updated_date", "rdf_type", "rdf_type_name", "name", "synonyms", "code",
    "production_year", "description", "species", "species_name","variety", 
    "variety_name", "accession", "accession_name", "institute", "website",
    GermplasmGetExportDTO.hasParentGermplasmFieldName, GermplasmGetExportDTO.hasParentMGermplasmFieldName, GermplasmGetExportDTO.hasParentFGermplasmFieldName,
    "metadata"})
public class GermplasmGetSingleDTO extends GermplasmGetExportDTO {

    /**
     * attributes
     */
    protected Map<String, String> metadata;

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
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
        dto.setRdfTypeName(model.getTypeLabel().getDefaultValue());
        dto.setName(model.getName());
        if (Objects.nonNull(model.getPublicationDate())) {
            dto.setPublicationDate(model.getPublicationDate());
        }
        if (Objects.nonNull(model.getLastUpdateDate())) {
            dto.setLastUpdatedDate(model.getLastUpdateDate());
        }

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

        if (model.getMetadata() != null) {
            dto.setMetadata(model.getMetadata().getAttributes());
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
