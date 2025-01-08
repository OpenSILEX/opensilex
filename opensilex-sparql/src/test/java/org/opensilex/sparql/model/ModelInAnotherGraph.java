/*
 * *****************************************************************************
 *                         ModelInAnotherGraph.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 23/08/2024 16:41
 * Contact: alexia.chiavarino@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 *
 *
 * *****************************************************************************
 *
 */

package org.opensilex.sparql.model;

import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;

import java.net.URI;

@SPARQLResource(
        ontology = TEST_ONTOLOGY.class,
        resource = "anotherResource",
        graph = TEST_ONTOLOGY.ANOTHER_GRAPH
)
public class ModelInAnotherGraph extends SPARQLResourceModel {

    @SPARQLProperty(
            ontology = TEST_ONTOLOGY.class,
            property = "hasLabel"
    )
    private URI inverseModelURI;

    public URI getInverseModelURI() {
        return inverseModelURI;
    }

    public void setInverseModelURI(URI inverseModelURI) {
        this.inverseModelURI = inverseModelURI;
    }
}
