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
        resource = "Characteristic",
        graph = VariableModel.GRAPH
)
public class CharacteristicModel extends BaseVariableModel<CharacteristicModel> {

    public CharacteristicModel() {

    }

    public CharacteristicModel(URI uri) {
        setUri(uri);
    }

    @Override
    public String[] getInstancePathSegments(CharacteristicModel instance) {
        return new String[]{
            "characteristic",
            instance.getName()
        };
    }
}
