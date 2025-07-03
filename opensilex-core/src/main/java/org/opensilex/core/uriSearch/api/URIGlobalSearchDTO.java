/*
 * ******************************************************************************
 *                                     URIGlobalSearchDTO.java
 *  OpenSILEX
 *  Copyright © INRAE 2024
 *  Creation date:  26 august, 2024
 *  Contact: maximilian.hart@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.uriSearch.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.core.data.api.DataFileGetDTO;
import org.opensilex.core.data.api.DataGetSearchDTO;
import org.opensilex.core.ontology.api.URITypesDTO;
import org.opensilex.core.uriSearch.dal.UriSearchSparqlDao;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.rest.validation.DateFormat;
import org.opensilex.sparql.model.VocabularyModel;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Object returned for UriSearch, contains everything needed from publication info to the data-dto in-case the uri was a data.
 * Dates are in String format as MongoModels use Instant whereas SPARQL uses OffsetDateTime
 */
public class URIGlobalSearchDTO {
    //#region: fields
    private URI uri;

    private String name;

    @JsonProperty("rdf_type")
    private URI type;

    private URI context;

    @JsonProperty("rdf_type_name")
    private String typeLabel;

    @JsonProperty("rdfs_comment")
    private String rdfsComment;

    @JsonProperty("publisher")
    private UserGetDTO publisher;

    @JsonProperty("publication_date")
    private String publicationDate;

    @JsonProperty("last_updated_date")
    private String lastUpdatedDate;

    @JsonProperty("super_types")
    private URITypesDTO superTypes;

    //To be able to show data details if the uri was a data
    @JsonProperty("data_dto")
    private DataGetSearchDTO dataDto;

    //To be able to show datafile
    @JsonProperty("datafile_dto")
    private DataFileGetDTO datafileDto;

    //Simple boolean for now for identification of a BatchHistory. As there is no rdfType for this
    //Replace with a dto if we ever want a show details button for BatchHistory
    @JsonProperty("is_batch_history")
    private boolean isBatchHistory = false;

    //This is used to identify which page we need to navigate to if the uri was a type or a property
    @JsonProperty("root_class")
    private URI rootClass;

    //This is used to identify if the matched vocabulary was a class or a property
    @JsonProperty("is_property")
    private boolean isProperty;

    //This is used to permit navigation to parent Factor if the uri was a Factor Level
    @JsonProperty("factor_uri")
    private URI factorUri;

    //#endregion
    //#region: private constants

    private final static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DateFormat.YMDTHMSMSX.toString());

    //#endregion
    //#region: Factory methods

    public static URIGlobalSearchDTO fromSparqlUriGlobalSearchResult(UriSearchSparqlDao.SparqlNamedResourceModelPlus result){
        URIGlobalSearchDTO dto = new URIGlobalSearchDTO();
        dto.setUri(result.getModel().getUri());
        dto.setType(result.getModel().getType());
        dto.setTypeLabel(result.getModel().getTypeLabel().getDefaultValue());
        dto.setName(result.getModel().getName());
        dto.setContext(result.getContext());
        dto.setFactorUri(result.getFactor());

        if(result.getRdfsComment() != null){
            dto.setRdfsComment(result.getRdfsComment());
        }

        if(result.getPublisher()!=null && result.getPublisher().getUri() != null){
            UserGetDTO publisherAsUser = new UserGetDTO();
            publisherAsUser.setFirstName(result.getPublisher().getFirstName());
            publisherAsUser.setLastName(result.getPublisher().getLastName());
            publisherAsUser.setUri(result.getModel().getPublisher());
            dto.setPublisher(publisherAsUser);
        }

        setMetadataDatesFromOffsetDatetimes(dto, result.getModel().getPublicationDate(), result.getModel().getLastUpdateDate());

        return dto;
    }

    /**
     * To be used when vocabulary for class or property from ontology matched the searched uri
     */
    public static URIGlobalSearchDTO fromVocabularyModel(VocabularyModel<?> model){
        URIGlobalSearchDTO dto = new URIGlobalSearchDTO();
        dto.setUri(model.getUri());
        dto.setName(model.getLabel().getDefaultValue());
        dto.setTypeLabel(model.getTypeLabel().getDefaultValue());
        dto.setType(model.getType());
        dto.setRdfsComment(model.getComment().getDefaultValue());
        setMetadataDatesFromOffsetDatetimes(dto, model.getPublicationDate(), model.getLastUpdateDate());
        return dto;
    }

    /**
     *
     * @param model
     * @return a URIGlobalSearchDTO with all fields completed except name and typename
     */
    public static URIGlobalSearchDTO fromMongoModel(MongoModel model){
        URIGlobalSearchDTO dto = new URIGlobalSearchDTO();
        dto.setUri(model.getUri());
        dto.setType(model.getRdfType());
        //TODO publicationDate and updateDate have been put to toString for now, the format isn't the same as in SPARQLResourceModel and these fields are currently always empty anyway
        if(model.getPublicationDate() != null){
            dto.setPublicationDate(model.getPublicationDate().toString());
        }
        if(model.getLastUpdateDate() != null){
            dto.setLastUpdatedDate(model.getLastUpdateDate().toString());
        }
        return dto;
    }

    //#endregion
    //#region: private methods

    private static void setMetadataDatesFromOffsetDatetimes(URIGlobalSearchDTO dto, OffsetDateTime publicationDate, OffsetDateTime updatedDate){
        if(publicationDate != null){
            dto.setPublicationDate(publicationDate.format(dateTimeFormatter));
        }
        if(updatedDate != null){
            dto.setLastUpdatedDate(updatedDate.format(dateTimeFormatter));
        }
    }

    //#endregion
    //#region: getters and setters

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

    public URIGlobalSearchDTO setTypeLabel(String typeLabel) {
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

    public URIGlobalSearchDTO setName(String name) {
        this.name = name;
        return this;
    }

    public URITypesDTO getSuperTypes() {
        return superTypes;
    }

    public void setSuperTypes(URITypesDTO superTypes) {
        this.superTypes = superTypes;
    }

    public DataGetSearchDTO getDataDto() {
        return dataDto;
    }

    public void setDataDto(DataGetSearchDTO dataDto) {
        this.dataDto = dataDto;
    }

    public DataFileGetDTO getDatafileDto() {
        return datafileDto;
    }

    public void setDatafileDto(DataFileGetDTO datafileDto) {
        this.datafileDto = datafileDto;
    }

    public String getRdfsComment() {
        return rdfsComment;
    }

    public void setRdfsComment(String rdfsComment) {
        this.rdfsComment = rdfsComment;
    }

    public URI getRootClass() {
        return rootClass;
    }

    public void setRootClass(URI rootClass) {
        this.rootClass = rootClass;
    }

    public boolean isProperty() {
        return isProperty;
    }

    public void setIsProperty(boolean isProperty) {
        this.isProperty = isProperty;
    }

    public URI getContext() {
        return context;
    }

    public void setContext(URI context) {
        this.context = context;
    }

    public URI getFactorUri() {
        return factorUri;
    }

    public void setFactorUri(URI factorUri) {
        this.factorUri = factorUri;
    }

    public boolean isBatchHistory() {
        return isBatchHistory;
    }

    public void setBatchHistory(boolean batchHistory) {
        isBatchHistory = batchHistory;
    }

    public void setProperty(boolean property) {
        isProperty = property;
    }

    //#endregion
}
