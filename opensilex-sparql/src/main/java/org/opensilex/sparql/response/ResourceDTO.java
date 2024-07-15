/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Objects;

import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 *
 * @author vince
 */
public abstract class ResourceDTO<T extends SPARQLResourceModel> {

    protected URI uri;

    @JsonProperty("rdf_type")
    protected URI type;

    @JsonProperty("rdf_type_name")
    protected String typeLabel;

    @JsonProperty("publication_date")
    private OffsetDateTime publicationDate;

    @JsonProperty("last_updated_date")
    private OffsetDateTime lastUpdatedDate;

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

    public void setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
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

    public abstract T newModelInstance();

    public T newModel() {
        T instance = newModelInstance();
        toModel(instance);
        return instance;
    }

    public void fromModel(T model) {
        setUri(model.getUri());
        setType(model.getType());
        if(model.getTypeLabel() != null){
            setTypeLabel(model.getTypeLabel().getDefaultValue());
        }
        if (Objects.nonNull(model.getPublicationDate())) {
            setPublicationDate(model.getPublicationDate());
        }
        if (Objects.nonNull(model.getLastUpdateDate())) {
            setLastUpdatedDate(model.getLastUpdateDate());
        }
    }

    public void toModel(T model) {
        model.setUri(getUri());
        model.setType(getType());
    }

}
