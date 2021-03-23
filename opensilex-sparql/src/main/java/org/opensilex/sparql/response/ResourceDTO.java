/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
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

    public abstract T newModelInstance();

    public T newModel() {
        T instance = newModelInstance();
        toModel(instance);
        return instance;
    }

    public void fromModel(T model) {
        setUri(model.getUri());
        setType(model.getType());
        setTypeLabel(model.getTypeLabel().getDefaultValue());
    }

    public void toModel(T model) {
        model.setUri(getUri());
        model.setType(getType());
    }

}
