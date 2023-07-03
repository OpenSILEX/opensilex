package org.opensilex.core.variable.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.sharedResource.SharedResourceInstanceDTO;
import org.opensilex.core.variable.dal.BaseVariableModel;

import java.net.URI;
import java.time.OffsetDateTime;

@JsonPropertyOrder({
        "uri", "name", "description",
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

    protected void setBasePropertiesToModel(T model) {
        setSkosReferencesToModel(model);
        model.setUri(this.getUri());
        model.setName(this.getName());
        model.setDescription(this.getDescription());
    }

    public abstract T toModel();
}
