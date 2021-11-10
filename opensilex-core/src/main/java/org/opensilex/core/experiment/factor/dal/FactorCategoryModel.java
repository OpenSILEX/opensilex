
package org.opensilex.core.experiment.factor.dal;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 * @author Arnaud Charleroy
 A simple model which define an instance of the FactorCategoryModel class
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "FactorCategory"
//        graph = "http://www.opensilex.org/vocabulary/set/factor/category"
)
public class FactorCategoryModel extends SPARQLResourceModel {

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label",
            ignoreUpdateIfNull = true,
            required = true
    )
    protected String name;
    public static final String NAME_FIELD = "name";

     public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
