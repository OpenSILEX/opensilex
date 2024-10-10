/*
 * ******************************************************************************
 *                                     BasicMongoSparqlDTO.java
 *  OpenSILEX
 *  Copyright © INRAE 2024
 *  Creation date:  26 august, 2024
 *  Contact: maximilian.hart@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.uriSearch.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.core.data.api.DataGetSearchDTO;
import org.opensilex.core.ontology.api.URITypesDTO;
import org.opensilex.core.uriSearch.dal.UriSearchSparqlDao;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.rest.validation.DateFormat;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Object returned for UriSearch, contains everything needed from publication info to the data-dto in-case the uri was a data.
 * Dates are in String format as MongoModels use Instant whereas SPARQL uses OffsetDateTime
 */
public class BasicMongoSparqlDTO {
    private URI uri;

    private String name;

    @JsonProperty("rdf_type")
    private URI type;

    @JsonProperty("number_total_matches")
    private int totalMatches;

    @JsonProperty("rdf_type_name")
    private String typeLabel;

    @JsonProperty("publisher")
    private UserGetDTO publisher;

    @JsonProperty("publication_date")
    private String publicationDate;

    @JsonProperty("last_updated_date")
    private String lastUpdatedDate;

    @JsonProperty("super_types")
    private List<URITypesDTO> superTypes;

    //To be able to show data details if the uri was a data
    @JsonProperty("data_dto")
    private DataGetSearchDTO dataDto;

    private final static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DateFormat.YMDTHMSMSX.toString());

    public static BasicMongoSparqlDTO fromSparqlUriGlobalSearchResult(UriSearchSparqlDao.SparqlNamedResourceModelWithExtraStuff result){
        BasicMongoSparqlDTO dto = new BasicMongoSparqlDTO();
        dto.setUri(result.getModel().getUri());
        dto.setType(result.getModel().getType());
        dto.setTypeLabel(result.getModel().getTypeLabel().getDefaultValue());
        dto.setName(result.getModel().getName());
        dto.setTotalMatches(result.getTotalMatches());

        if(result.getPublisher()!=null && result.getPublisher().getUri() != null){
            UserGetDTO publisherAsUser = new UserGetDTO();
            publisherAsUser.setFirstName(result.getPublisher().getFirstName());
            publisherAsUser.setLastName(result.getPublisher().getLastName());
            publisherAsUser.setUri(result.getModel().getPublisher());
            dto.setPublisher(publisherAsUser);
        }

        if(result.getModel().getPublicationDate() != null){
            dto.setPublicationDate(result.getModel().getPublicationDate().format(dateTimeFormatter));
        }
        if(result.getModel().getLastUpdateDate() != null){
            dto.setLastUpdatedDate(result.getModel().getLastUpdateDate().format(dateTimeFormatter));
        }
        return dto;
    }

    /**
     *
     * @param model
     * @return a BasicMongoSparqlDTO with all fields completed except name and typename
     */
    public static BasicMongoSparqlDTO fromMongoModel(MongoModel model){
        BasicMongoSparqlDTO dto = new BasicMongoSparqlDTO();
        dto.setUri(model.getUri());
        dto.setType(model.getRdfType());
        dto.setTotalMatches(1);
        if(model.getPublicationDate() != null){
            dto.setPublicationDate(model.getPublicationDate().toString());
        }
        if(model.getLastUpdateDate() != null){
            dto.setLastUpdatedDate(model.getLastUpdateDate().toString());
        }
        return dto;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public BasicMongoSparqlDTO setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
        return this;
    }

    public UserGetDTO getPublisher() {
        return publisher;
    }

    public void setPublisher(UserGetDTO publisher) {
        this.publisher = publisher;
    }

    public String getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(String publicationDate) {
        this.publicationDate = publicationDate;
    }

    public String getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(String lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    public String getName() {
        return name;
    }

    public BasicMongoSparqlDTO setName(String name) {
        this.name = name;
        return this;
    }

    public List<URITypesDTO> getSuperTypes() {
        return superTypes;
    }

    public void setSuperTypes(List<URITypesDTO> superTypes) {
        this.superTypes = superTypes;
    }

    public DataGetSearchDTO getDataDto() {
        return dataDto;
    }

    public void setDataDto(DataGetSearchDTO dataDto) {
        this.dataDto = dataDto;
    }

    public int getTotalMatches() {
        return totalMatches;
    }

    public void setTotalMatches(int totalMatches) {
        this.totalMatches = totalMatches;
    }
}
