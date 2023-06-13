//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.variable.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.variable.dal.BaseMultiLabeledIdentifierModel;
import org.opensilex.server.rest.validation.ValidURI;

import java.net.URI;

@JsonPropertyOrder({
        "uri", "multiLabelDTO",
        SKOSReferencesDTO.EXACT_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.CLOSE_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.BROAD_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.NARROW_MATCH_JSON_PROPERTY
})
public abstract class BaseMultiLabeledIdentifierCreationDTO<T extends BaseMultiLabeledIdentifierModel<T>> extends SKOSReferencesDTO {

    @JsonProperty("uri")
    protected URI uri;

    @JsonProperty("multiLabelDTO")
    public MultiLabelDTO multiLabelDTO;

    public BaseMultiLabeledIdentifierCreationDTO() {


    }

    @ValidURI
    public URI getUri() {
        return uri;
    }


    public void setUri(URI uri) {
        this.uri = uri;
    }

    public MultiLabelDTO getMultiLabelDTO() {
        return multiLabelDTO;
    }

    public void setMultiLabelDTO(MultiLabelDTO multiLabelDTO) {
        this.multiLabelDTO = multiLabelDTO;
    }

    public T newModel() {
        T model = newModelInstance();
        return model;
    }

    protected abstract T newModelInstance();
}
