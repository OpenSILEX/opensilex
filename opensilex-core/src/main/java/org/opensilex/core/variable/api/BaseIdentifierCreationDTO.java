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
import org.opensilex.core.variable.dal.BaseIdentifierModel;
import org.opensilex.core.variable.dal.BaseVariableModel;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.server.rest.validation.ValidURI;

import java.net.URI;
import java.util.List;

@JsonPropertyOrder({
        "uri", "labelDTOs",
        SKOSReferencesDTO.EXACT_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.CLOSE_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.BROAD_MATCH_JSON_PROPERTY,
        SKOSReferencesDTO.NARROW_MATCH_JSON_PROPERTY
})
public abstract class BaseIdentifierCreationDTO<T extends BaseIdentifierModel<T>> extends SKOSReferencesDTO {

    @JsonProperty("uri")
    protected URI uri;

    @JsonProperty("labelDTOs")
    protected List<LabelDTO> labelDTOs;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public List<LabelDTO> getLabelDTOs() {
        return labelDTOs;
    }

    public void setLabelDTOs(List<LabelDTO> labelDTOs) {
        this.labelDTOs = labelDTOs;
    }

    public T newModel() {

        T model = newModelInstance();
        model.setUri(uri);
        model.setLabelDTOs(labelDTOs);

        setSkosReferencesToModel(model);
        return model;
    }


    protected abstract T newModelInstance();
}
