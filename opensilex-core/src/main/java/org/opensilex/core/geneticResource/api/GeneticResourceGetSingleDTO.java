//******************************************************************************
//                          GeneticResourceGetSingleDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.geneticResource.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.opensilex.core.geneticResource.dal.GeneticResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 * DTO representing JSON for searching geneticResource or getting them by uri
 *
 * @author Alice Boizet
 */
@JsonPropertyOrder({"uri", "publisher", "publication_date", "last_updated_date", "rdf_type", "rdf_type_name", "name", "synonyms", "code",
    "production_year", "description", "species", "species_name","variety",
    "variety_name", "accession", "accession_name", "institute", "website","is_public", "groups",
     GeneticResourceGetExportDTO.hasParentGeneticResourceFieldName, GeneticResourceGetExportDTO.hasParentMGeneticResourceFieldName, GeneticResourceGetExportDTO.hasParentFGeneticResourceFieldName,
     "metadata"})
public class GeneticResourceGetSingleDTO extends GeneticResourceGetExportDTO {

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
     * Convert GeneticResource Model into GeneticResource DTO
     *
     * @param model GeneticResource Model to convert
     * @return Corresponding user DTO
     */
    public static GeneticResourceGetSingleDTO fromModel(GeneticResourceModel model) {
        GeneticResourceGetSingleDTO dto = new GeneticResourceGetSingleDTO();

        dto.setUri(model.getUri());
        dto.setRdfType(model.getType());
        dto.setRdfTypeName(model.getTypeLabel().getDefaultValue());
        dto.setName(model.getName());
        dto.setGroups(SPARQLResourceModel.getUriList(model.getGroups()));
        if ((model.getIsPublic()) != null) {
            dto.setIsPublic(model.getIsPublic());
        }else{
            dto.setIsPublic(true);
        }
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
