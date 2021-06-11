package org.opensilex.core.event.api.csv;

import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.security.user.dal.UserModel;

import java.io.InputStream;
import java.net.URI;

public class EventCsvImporter extends AbstractEventCsvImporter<EventModel> {

    public EventCsvImporter(OntologyDAO ontologyDAO, InputStream file, UserModel user){
        super(ontologyDAO,file, user);
    }

    @Override
    protected EventModel getNewModel() {
        return new EventModel();
    }
}
