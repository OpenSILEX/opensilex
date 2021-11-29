//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: hamza.ikiou@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variablesGroup.dal;

import java.util.List;

import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;

import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.variable.dal.VariableModel;

import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

/**
 * @author Hamza IKIOU
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "VariablesGroup",
        graph = VariablesGroupModel.GRAPH
)
public class VariablesGroupModel extends SPARQLNamedResourceModel<VariablesGroupModel> {

    public static final String GRAPH = "variablesGroup";

    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "description"
    )
    private String description;
    public static final String DESCRIPTION_FIELD = "description";
    
    @SPARQLProperty(
            ontology = RDFS.class,
            property = "member"
    )
    private List<VariableModel> variablesList;
    public static final String VARIABLES_LIST_FIELD = "variablesList";
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<VariableModel> getVariablesList() {
        return variablesList;
    }
    
    public void setVariablesList(List<VariableModel> variables) {
        this.variablesList = variables;
    }
}
