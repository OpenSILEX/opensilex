/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.dal;

import java.net.URI;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.utils.ClassURIGenerator;


@SPARQLResource(
        ontology = Oeso.class,
        resource = "Unit",
        graph = "variable"
)
public class UnitModel extends BaseVariableModel implements ClassURIGenerator<UnitModel> {

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasDimension"
    )
    private String dimension;
    public static final String DIMENSION_FIELD_NAME = "dimension";

    public UnitModel() {

    }

    public UnitModel(URI uri) {
        setUri(uri);
    }

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    @Override
    public String[] getUriSegments(UnitModel instance) {
        return new String[]{
            "variable",
            "unit",
            instance.getLabel()
        };
    }
}
