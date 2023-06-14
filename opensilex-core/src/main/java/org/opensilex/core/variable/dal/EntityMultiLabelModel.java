//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.dal;

//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************

import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLResource;

import java.net.URI;

/**
 * @author vidalmor
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "Entity",
        graph = VariableModel.GRAPH
)
public class EntityMultiLabelModel extends BaseMultiLabeledIdentifierModel<EntityMultiLabelModel> {

    public EntityMultiLabelModel() {

    }

    public EntityMultiLabelModel(URI uri) {
        setUri(uri);

    }

    public String[] getInstancePathSegments(EntityMultiLabelModel instance) {
        System.out.println(instance);
        return new String[]{
                "entity",
                instance.getPrefLabels().getTranslations().values().stream().findAny().get()
        };
    }


}

