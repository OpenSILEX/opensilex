//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.dal;

import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.variable.api.LabelDTO;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.group.dal.GroupUserProfileModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;

import java.net.URI;
import java.util.List;

/**
 * @author vidalmor
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "Entity",
        graph = VariableModel.GRAPH
)
public class EntityModel extends BaseIdentifierModel<EntityModel> {


    public EntityModel() {
    }

    public EntityModel(URI uri) {
        setUri(uri);
    }


    @Override
    public String[] getInstancePathSegments(EntityModel instance) {
        return new String[]{
                "entity",
                instance.getName()
        };
    }
}
