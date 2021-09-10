//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: hamza.ikiou@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variablesGroup.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.core.variablesGroup.dal.VariablesGroupModel;
import org.opensilex.core.variable.api.VariableGetDTO;

/**
 * @author Hamza IKIOU
 */
public class VariablesGroupGetDTO extends VariablesGroupDTO {
    
        @JsonProperty("variables")
        protected List<VariableGetDTO> variablesList = new ArrayList<>();

        public List<VariableGetDTO> getVariablesList() {
            return variablesList;
        }

        public void setVariablesList(List<VariableGetDTO> variablesList) {
            this.variablesList = variablesList;
        }
        
        public static VariablesGroupGetDTO fromModel(VariablesGroupModel model) {

        VariablesGroupGetDTO dto = new VariablesGroupGetDTO();

        dto.setUri(model.getUri());
        dto.setName(model.getName());
        dto.setDescription(model.getDescription());
        
        List<VariableGetDTO> variableList = new ArrayList<>();
        for (VariableModel variableModel : model.getVariablesList()) {
            variableList.add(VariableGetDTO.fromModel(variableModel));
        }
        dto.setVariablesList(variableList);

        return dto;
    }
}
