/*
 * *****************************************************************************
 *                         InverseModel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 23/08/2024 16:50
 * Contact: alexia.chiavarino@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 *
 *
 * *****************************************************************************
 *
 */

package org.opensilex.sparql.model;

import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;

@SPARQLResource(
        ontology = TEST_ONTOLOGY.class,
        resource = "inverse",
        graph = TEST_ONTOLOGY.GRAPH_SUFFIX
)
public class InverseModel extends SPARQLResourceModel {

    @SPARQLProperty(
            ontology = TEST_ONTOLOGY.class,
            property = "hasLabel",
            inverse = true
    )
    private ModelInAnotherGraph anotherModel;

    public ModelInAnotherGraph getAnotherModel() {
        return anotherModel;
    }

    public void setAnotherModel(ModelInAnotherGraph anotherModel) {
        this.anotherModel = anotherModel;
    }
}
