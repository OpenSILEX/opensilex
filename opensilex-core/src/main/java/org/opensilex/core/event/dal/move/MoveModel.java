package org.opensilex.core.event.dal.move;

import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.sparql.annotations.SPARQLResource;


/**
 * @author Renaud COLIN
 */
@SPARQLResource(
        ontology = Oeev.class,
        resource = "Move",
        graph = EventModel.GRAPH
)
public class MoveModel extends EventModel {

    private LocationObservationModel locationObservation;

    public LocationObservationModel getLocationObservation() {
        return locationObservation;
    }

    public void setLocationObservation(LocationObservationModel locationObservation) {
        this.locationObservation = locationObservation;
    }
}
