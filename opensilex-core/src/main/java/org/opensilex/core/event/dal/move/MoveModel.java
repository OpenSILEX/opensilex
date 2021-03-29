package org.opensilex.core.event.dal.move;

import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.sparql.annotations.SPARQLIgnore;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;

/**
 * @author Renaud COLIN
 */
@SPARQLResource(
        ontology = Oeev.class,
        resource = "Move",
        graph = "events",
        prefix = "ev"
)
public class MoveModel extends EventModel {

    @SPARQLProperty(
            ontology = Oeev.class,
            property = "to"
    )
    private InfrastructureFacilityModel to;
    public static final String TO_FIELD = "to";


    @SPARQLProperty(
            ontology = Oeev.class,
            property = "from"
    )
    private InfrastructureFacilityModel from;
    public static final String FROM_FIELD = "from";


    @SPARQLIgnore
    private MoveEventNoSqlModel noSqlModel;

    public InfrastructureFacilityModel getTo() {
        return to;
    }

    public void setTo(InfrastructureFacilityModel to) {
        this.to = to;
    }

    public InfrastructureFacilityModel getFrom() {
        return from;
    }

    public void setFrom(InfrastructureFacilityModel from) {
        this.from = from;
    }


    public MoveEventNoSqlModel getNoSqlModel() {
        return noSqlModel;
    }

    public void setNoSqlModel(MoveEventNoSqlModel noSqlModel) {
        this.noSqlModel = noSqlModel;
    }
}
