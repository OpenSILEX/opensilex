//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: hamza.ikiou@inrae.fr, renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variable.dal;

import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLResource;

import java.net.URI;

/**
 * @author Hamza IKIOU
 * @author Renaud COLIN
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "EntityOfInterest",
        graph = VariableModel.GRAPH
)
public class InterestEntityModel extends BaseVariableModel<InterestEntityModel> {

    public InterestEntityModel() {
    }

    public InterestEntityModel(URI uri) {
        setUri(uri);
    }
    
    @Override
    public String[] getInstancePathSegments(InterestEntityModel instance) {
        return new String[]{
                "entity_of_interest",
                instance.getName()
        };
    }    
}
