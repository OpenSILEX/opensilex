package org.opensilex.core.event.dal.move;

import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.sparql.annotations.SPARQLIgnore;
import org.opensilex.sparql.annotations.SPARQLProperty;
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

    //TOD: a supprimer
    @SPARQLProperty(
            ontology = Oeev.class,
            property = "to"
    )
    private FacilityModel to;
    public static final String TO_FIELD = "to";


    @SPARQLProperty(
            ontology = Oeev.class,
            property = "from"
    )
    private FacilityModel from;
    public static final String FROM_FIELD = "from";


    @SPARQLIgnore
    private MoveNosqlModel noSqlModel;

    public FacilityModel getTo() {
        return to;
    }

    public void setTo(FacilityModel to) {
        this.to = to;
    }

    public FacilityModel getFrom() {
        return from;
    }

    public void setFrom(FacilityModel from) {
        this.from = from;
    }

    public MoveNosqlModel getNoSqlModel() {
        return noSqlModel;
    }

    public void setNoSqlModel(MoveNosqlModel noSqlModel) {
        this.noSqlModel = noSqlModel;
    }

    public LocationObservationModel getLocationObservation() {
        return locationObservation;
    }

    public void setLocationObservation(LocationObservationModel locationObservation) {
        this.locationObservation = locationObservation;
    }
}
