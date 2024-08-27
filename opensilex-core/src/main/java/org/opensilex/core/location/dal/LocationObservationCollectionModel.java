/*
 * *****************************************************************************
 *                         LocationObservationCollectionModel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 26/07/2024 13:35
 * Contact: alexia.chiavarino@inrae.fr
 * *****************************************************************************
 *
 */

package org.opensilex.core.location.dal;

import org.opensilex.core.ontology.SOSA;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.uri.generation.ClassURIGenerator;

import java.net.URI;
import java.util.UUID;

@SPARQLResource(
        ontology = SOSA.class,
        resource = "ObservationCollection",
        graph = LocationObservationCollectionModel.GRAPH
)
public class LocationObservationCollectionModel  extends SPARQLResourceModel implements ClassURIGenerator<LocationObservationCollectionModel> {
    public static final String GRAPH = "ObservationCollection";

    @SPARQLProperty(
            ontology = SOSA.class,
            property = "hasFeatureOfInterest"
    )
    private URI featureOfInterest;

    public URI getFeatureOfInterest() {
        return featureOfInterest;
    }

    public void setFeatureOfInterest(URI featureOfInterest) {
        this.featureOfInterest = featureOfInterest;
    }

    @Override
    public String[] getInstancePathSegments(LocationObservationCollectionModel instance) {
        return new String[]{
                UUID.randomUUID().toString()
        };
    }
}
