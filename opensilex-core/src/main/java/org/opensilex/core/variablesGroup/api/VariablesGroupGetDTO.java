//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: hamza.ikiou@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variablesGroup.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.core.variablesGroup.dal.VariablesGroupModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.response.NamedResourceDTO;

/**
 * @author Hamza IKIOU
 */
public class VariablesGroupGetDTO extends VariablesGroupDTO {

    @JsonProperty("variables")
    protected List<NamedResourceDTO<VariableModel>> variablesList = new ArrayList<>();

    public List<NamedResourceDTO<VariableModel>> getVariablesList() {
        return variablesList;
    }

    public void setVariablesList(List<NamedResourceDTO<VariableModel>> variablesList) {
        this.variablesList = variablesList;
    }

    public static VariablesGroupGetDTO fromModel(VariablesGroupModel model) {

        VariablesGroupGetDTO dto = new VariablesGroupGetDTO();

        try {
            dto.setUri(new URI(SPARQLDeserializers.getShortURI(model.getUri())));
        } catch (URISyntaxException e) {
            dto.setUri(model.getUri());
        }
        dto.setName(model.getName());
        dto.setDescription(model.getDescription());
        if (Objects.nonNull(model.getPublicationDate())) {
            dto.setPublicationDate(model.getPublicationDate());
        }
        if (Objects.nonNull(model.getLastUpdateDate())) {
            dto.setLastUpdatedDate(model.getLastUpdateDate());
        }
        List<NamedResourceDTO<VariableModel>> variableList = new ArrayList<>(model.getVariablesList().size());

        for (VariableModel variableModel : model.getVariablesList()) {
            variableList.add(NamedResourceDTO.getDTOFromModel(variableModel));
        }
        dto.setVariablesList(variableList);

        return dto;
    }
}
