/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.germplasm.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 *
 * @author Alice Boizet
 */
@JsonPropertyOrder({"uri", "rdf_type", "rdf_type_name", "name", "species", "species_name","is_public","groups_users",})
public class GermplasmGetAllDTO {

    /**
     * Germplasm URI
     */
    protected URI uri;

    @JsonProperty("publisher")
    protected UserGetDTO publisher;

    @JsonProperty("publication_date")
    protected OffsetDateTime publicationDate;

    @JsonProperty("last_updated_date")
    protected OffsetDateTime lastUpdatedDate;

    /**
     * Germplasm Type : Species, Variety, Accession or subclass of PlantMaterialLot
     */
    @JsonProperty("rdf_type")
    protected URI rdfType;

    /**
     * typeLabel
     */
    @JsonProperty("rdf_type_name")
    protected String rdfTypeName;

    /**
     * Germplasm name
     */
    protected String name;

    /**
     * Germplasm species URI
     */
    protected URI species;

    /**
     * speciesLabel
     */
    @JsonProperty("species_name")
    protected String speciesName;

    @JsonProperty("is_public")
    protected Boolean isPublic;

    @JsonProperty("groups_users")
    protected List<URI> groupsUsers = new ArrayList<>();

    public List<URI> setGroupsUsers() {
        return groupsUsers;
    }

    public void setGroupsUsers(List<URI> groups) {
        this.groupsUsers = groups;
    }


    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public URI getRdfType() {
        return rdfType;
    }

    public void setRdfType(URI rdfType) {
        this.rdfType = rdfType;
    }

    public String getRdfTypeName() {
        return rdfTypeName;
    }

    public void setRdfTypeName(String rdfTypeName) {
        this.rdfTypeName = rdfTypeName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URI getSpecies() {
        return species;
    }

    public void setSpecies(URI species) {
        this.species = species;
    }

    public String getSpeciesName() {
        return speciesName;
    }

    public void setSpeciesName(String speciesName) {
        this.speciesName = speciesName;
    }

    public UserGetDTO getPublisher() {
        return publisher;
    }

    public void setPublisher(UserGetDTO publisher) {
        this.publisher = publisher;
    }

    public OffsetDateTime getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(OffsetDateTime publicationDate) {
        this.publicationDate = publicationDate;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public OffsetDateTime getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(OffsetDateTime lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    protected static List<URI> getUriList(List<? extends SPARQLResourceModel> models) {

        if (models == null || models.isEmpty()) {
            return Collections.emptyList();
        }
        return models.stream().map(SPARQLResourceModel::getUri)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Convert Germplasm Model into Germplasm DTO
     *
     * @param model Germplasm Model to convert
     * @return Corresponding user DTO
     */
    public static GermplasmGetAllDTO fromModel(GermplasmModel model) {
        GermplasmGetAllDTO dto = new GermplasmGetAllDTO();

        dto.setUri(model.getUri());
        dto.setRdfType(model.getType());
        dto.setRdfTypeName(model.getTypeLabel().getDefaultValue());
        dto.setName(model.getName());
        dto.setIsPublic(model.getIsPublic());
        dto.setGroupsUsers(getUriList(model.getGroupsUsers()));

        if (model.getSpecies() != null) {
            dto.setSpecies(model.getSpecies().getUri());
            try {
                dto.setSpeciesName(model.getSpecies().getName());
            } catch (Exception e) {
            }
        }

        return dto;
    }

}
