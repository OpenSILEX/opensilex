/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.dal;

import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;

import java.net.URI;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "Unit",
        graph = VariableModel.GRAPH
)
public class UnitModel extends BaseVariableModel<UnitModel> {

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasSymbol"
    )
    private String symbol;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasAlternativeSymbol"
    )
    private String alternativeSymbol;

    public UnitModel() {

    }

    public UnitModel(URI uri) {
        setUri(uri);
    }

    @Override
    public String[] getInstancePathSegments(UnitModel instance) {
        return new String[]{
            "unit",
            instance.getName()
        };
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getAlternativeSymbol() {
        return alternativeSymbol;
    }

    public void setAlternativeSymbol(String alternativeSymbol) {
        this.alternativeSymbol = alternativeSymbol;
    }

}
