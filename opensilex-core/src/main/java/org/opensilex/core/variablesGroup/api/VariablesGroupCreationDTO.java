//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: hamza.ikiou@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variablesGroup.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.net.URI;

import java.util.ArrayList;
import java.util.List;

import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.core.variablesGroup.dal.VariablesGroupModel;

/**
 * @author Hamza IKIOU
 */
@JsonPropertyOrder({"uri", "name", "description","variables"})
public class VariablesGroupCreationDTO extends VariablesGroupDTO{
    
    @JsonProperty("variables")
    protected List<URI> variablesList = new ArrayList<>();
    
    public List<URI> getVariablesList() {
        return variablesList;
    }
    
    public void setVariablesList(List<URI> variablesList) {
        this.variablesList = variablesList;
    }
    
    public VariablesGroupModel newModel(){
        
        VariablesGroupModel model = new VariablesGroupModel();
        
        model.setUri(getUri());
        
        model.setName(name);
        model.setDescription(description);
        List<VariableModel> variableList = new ArrayList<>(variablesList.size());
        
        variablesList.forEach((URI uri) -> {
            VariableModel variableModel = new VariableModel();
            variableModel.setUri(uri);
            variableList.add(variableModel);
            
        });
        model.setVariablesList(variableList);
        
        return model;
    }
}
