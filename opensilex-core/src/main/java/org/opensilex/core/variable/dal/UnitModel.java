/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.dal;

import java.net.*;
import org.opensilex.core.ontology.*;
import org.opensilex.sparql.annotations.*;
import org.opensilex.sparql.utils.*;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "Unit",
        graph = "variable"
)
public class UnitModel extends BaseVariableModel implements ClassURIGenerator<UnitModel> {

    public UnitModel() {

    }

    public UnitModel(URI uri) {
        setUri(uri);
    }

    @Override
    public String[] getUriSegments(UnitModel instance) {
        return new String[]{
            "variable",
            "unit",
            instance.getName()
        };
    }
}
