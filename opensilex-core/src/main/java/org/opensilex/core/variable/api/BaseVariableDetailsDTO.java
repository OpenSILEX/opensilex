package org.opensilex.core.variable.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.sharedResource.SharedResourceInstanceDTO;
import org.opensilex.core.variable.dal.BaseVariableModel;
import org.opensilex.security.user.api.UserGetDTO;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Objects;

@JsonPropertyOrder({
        "uri", "name", "description", "publisher", "publication_date", "last_updated_date",
        SKOSReferencesDTO.EXACT_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.CLOSE_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.BROAD_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.NARROW_MATCH_JSON_PROPERTY
})
public abstract class BaseVariableDetailsDTO<T extends BaseVariableModel<T>> extends SKOSReferencesDTO {

    protected BaseVariableDetailsDTO(T model, SharedResourceInstanceDTO sharedResourceInstance) {
        this(model);
        setFromSharedResourceInstance(sharedResourceInstance);
    }

    protected BaseVariableDetailsDTO(T model){
        uri = model.getUri();
        name = model.getName();
        description = model.getDescription();
        publicationDate = model.getPublicationDate();
        if (Objects.nonNull(model.getLastUpdateDate())) {
            lastUpdatedDate = model.getLastUpdateDate();
        }
        setSkosReferencesFromModel(model);
    }

    protected BaseVariableDetailsDTO(){

    }

    @JsonProperty("uri")
    protected URI uri;

    @JsonProperty("name")
    protected String name;

    @JsonProperty("description")
    protected String description;

    @JsonProperty("publisher")
    protected UserGetDTO publisher;

    @JsonProperty("publication_date")
    protected OffsetDateTime publicationDate;

    @JsonProperty("last_updated_date")
    protected OffsetDateTime lastUpdatedDate;

    @JsonProperty("from_shared_resource_instance")
    protected SharedResourceInstanceDTO fromSharedResourceInstance;

    public URI getUri() {
        return uri;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SharedResourceInstanceDTO getFromSharedResourceInstance() {
        return fromSharedResourceInstance;
    }

    public void setFromSharedResourceInstance(SharedResourceInstanceDTO fromSharedResourceInstance) {
        this.fromSharedResourceInstance = fromSharedResourceInstance;
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

    public OffsetDateTime getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(OffsetDateTime lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    protected void setBasePropertiesToModel(T model) {
        setSkosReferencesToModel(model);
        model.setUri(this.getUri());
        model.setName(this.getName());
        model.setDescription(this.getDescription());
    }

    public abstract T toModel();
}
