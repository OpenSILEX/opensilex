//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.dal;

import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLResource;

import java.net.URI;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "Method",
        graph = VariableModel.GRAPH
)
public class MethodModel extends BaseVariableModel<MethodModel> {

    public MethodModel() {

    }

    public MethodModel(URI uri) {
        setUri(uri);
    }

    @Override
    public String[] getUriSegments(MethodModel instance) {
        return new String[]{
            "method",
            instance.getName()
        };
    }
}
