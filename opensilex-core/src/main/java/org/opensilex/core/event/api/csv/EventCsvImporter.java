package org.opensilex.core.event.api.csv;

import org.opensilex.core.event.dal.EventModel;

import java.io.InputStream;
import java.net.URI;

public class EventCsvImporter extends AbstractEventCsvImporter<EventModel> {

    public EventCsvImporter(InputStream file, URI creator){
        super(file, creator);
    }

    @Override
    protected EventModel getNewModel() {
        return new EventModel();
    }
}
