package org.opensilex.core.variable.api;

//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//***********************************************************************
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.variable.dal.BaseVariableModel;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.server.rest.validation.ValidURI;

import java.net.URI;

@JsonPropertyOrder({
        "uri", "definition",
        SKOSReferencesDTO.EXACT_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.CLOSE_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.BROAD_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.NARROW_MATCH_JSON_PROPERTY
})
public abstract class BaseIdentifierCreationDTO<T extends BaseVariableModel<T>> extends SKOSReferencesDTO {

    @JsonProperty("uri")
    protected URI uri;

//    @Required
//    @JsonProperty("prefLabel")
//    protected String prefLabel;

    @JsonProperty("definition")
    protected String definition;

    @ValidURI
    public URI getUri() {
        return uri;
    }

//    @ApiModelProperty(required = true)
//    public String getName() {
//        return name;
//    }

    public String getDefinition() {
        return definition;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

//    public void setName(String name) {
//        this.name = name;
//    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }


    public T newModel(){
        T model = newModelInstance();
//        model.setName(name);
        if(!StringUtils.isEmpty(definition)){
            model.setDescription(definition);
        }
        model.setDescription(definition);
        model.setUri(uri);
        setSkosReferencesToModel(model);
        return model;
    }

    protected abstract T newModelInstance();
}
