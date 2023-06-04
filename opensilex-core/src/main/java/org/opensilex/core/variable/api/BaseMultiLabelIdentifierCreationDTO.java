//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.variable.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.variable.dal.BaseMultiLabelIdentifierModel;
import org.opensilex.server.rest.validation.ValidURI;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@JsonPropertyOrder({
        "uri", "labelDTOs",
        SKOSReferencesDTO.EXACT_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.CLOSE_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.BROAD_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.NARROW_MATCH_JSON_PROPERTY
})
//public abstract class BaseVariableCreationDTO<T extends BaseVariableModel<T>> extends SKOSReferencesDTO {
public abstract class BaseMultiLabelIdentifierCreationDTO<T extends BaseMultiLabelIdentifierModel<T>> extends SKOSReferencesDTO {

    @JsonProperty("uri")
    protected URI uri;

    @JsonProperty("prefLabels")
    protected List<String> prefLabels;

    @JsonProperty("altLabels")
    protected List<String> altLabels;

    @JsonProperty("definitions")
    protected List<String> definitions;

    public BaseMultiLabelIdentifierCreationDTO() {

        this.prefLabels = new ArrayList<String>();
        this.altLabels = new ArrayList<String>();
        this.definitions = new ArrayList<String>();
    }

    @ValidURI
    public URI getUri() {
        return uri;
    }


    public void setUri(URI uri) {
        this.uri = uri;
    }

    public List<String> getPrefLabels() {
        return prefLabels;
    }

    public void setPrefLabels(List<String> prefLabels) {
        this.prefLabels = prefLabels;
    }

    public List<String> getAltLabels() {
        return altLabels;
    }

    public void setAltLabels(List<String> altLabels) {
        this.altLabels = altLabels;
    }

    public List<String> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(List<String> definitions) {
        this.definitions = definitions;
    }

    public T newModel() {
        T model = newModelInstance();
        return model;
    }

    protected abstract T newModelInstance();
}
