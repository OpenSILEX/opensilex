//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.variable.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.core.ontology.OntologyReference;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.variable.dal.BaseVariableModel;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.server.rest.validation.ValidURI;

import java.net.URI;
import java.util.List;

@JsonPropertyOrder({
        "uri", "name", "description",
        "exactMatch","closeMatch","broader","narrower"
})
public abstract class BaseVariableCreationDTO<T extends BaseVariableModel<T>> extends SKOSReferencesDTO {

    @JsonProperty("uri")
    protected URI uri;

    @Required
    @JsonProperty("name")
    protected String name;

    @JsonProperty("description")
    protected String description;

    @ValidURI
    public URI getUri() {
        return uri;
    }

    @ApiModelProperty(required = true)
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


    public T newModel(){
        T model = newModelInstance();
        model.setName(name);
        if(!StringUtils.isEmpty(description)){
            model.setDescription(description);
        }
        model.setDescription(description);
        model.setUri(uri);
        setSkosReferencesToModel(model);
        return model;
    }

    protected abstract T newModelInstance();
}
