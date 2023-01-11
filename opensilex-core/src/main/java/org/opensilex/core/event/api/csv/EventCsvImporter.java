package org.opensilex.core.event.api.csv;

import org.opensilex.core.event.dal.EventModel;
import org.opensilex.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.sparql.exceptions.SPARQLMapperNotFoundException;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.service.SPARQLService;

import java.io.InputStream;

public class EventCsvImporter extends AbstractEventCsvImporter<EventModel> {

    public EventCsvImporter(SPARQLService sparql, OntologyDAO ontologyDAO, InputStream file, AccountModel user) throws SPARQLInvalidClassDefinitionException, SPARQLMapperNotFoundException {
        super(sparql,ontologyDAO,file, user);
    }

    @Override
    protected EventModel getNewModel() {
        return new EventModel();
    }
}
