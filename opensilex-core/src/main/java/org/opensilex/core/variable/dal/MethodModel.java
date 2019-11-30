//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.dal;

import java.net.*;
import org.opensilex.core.ontology.*;
import org.opensilex.sparql.annotations.*;
import org.opensilex.sparql.utils.*;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "Method",
        graph = "variable"
)
public class MethodModel extends BaseVariableModel implements ClassURIGenerator<MethodModel> {

    public MethodModel() {

    }

    public MethodModel(URI uri) {
        setUri(uri);
    }

    @Override
    public String[] getUriSegments(MethodModel instance) {
        return new String[]{
            "variable",
            "method",
            instance.getName()
        };
    }
}
