package org.opensilex.migration;


import org.opensilex.OpenSilex;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.update.OpenSilexModuleUpdate;
import org.opensilex.update.OpensilexModuleUpdateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;

public class RemoveExternalOntologiesContexts implements OpenSilexModuleUpdate {
    private OpenSilex opensilex;

    private SPARQLService sparql;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public OffsetDateTime getDate() {
        return OffsetDateTime.now();
    }

    @Override
    public String getDescription() {
        return "Remove contexts representing external ontologies";
    }

    @Override
    public void setOpensilex(OpenSilex opensilex) {
        this.opensilex = opensilex;
    }
    @Override
    public void execute() throws OpensilexModuleUpdateException {
        SPARQLServiceFactory factory = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        sparql = factory.provide();
        removeExternalOntologiesContexts();
    }

    private void removeExternalOntologiesContexts() {
        try {
            sparql.startTransaction();

            // Remove contexts representing external ontologies
            sparql.clearGraph("http://www.opensilex.org/security");
            sparql.clearGraph("http://www.w3.org/ns/org");
            sparql.clearGraph("http://xmlns.com/foaf/0.1/");
            sparql.clearGraph("http://www.w3.org/2006/vcard/ns");
            sparql.clearGraph("http://www.w3.org/ns/oa");
            sparql.clearGraph("http://www.opensilex.org/vocabulary/oeev");
            sparql.clearGraph("http://www.w3.org/2002/07/owl");
            sparql.clearGraph("http://www.w3.org/2006/time");
            sparql.clearGraph("http://purl.org/dc/terms/");

            sparql.commitTransaction();
            logger.info("Removed contexts representing external ontologies successfully");
        } catch (Exception e) {
            try {
                sparql.rollbackTransaction();
                logger.error("Error occurred while removing contexts representing external ontologies, no changes were made to the database", e);
            } catch (Exception exception) {
                logger.error("Critical error occurred while removing contexts representing external ontologies, the database may no longer be in a stable state, data may have been lost", exception);
            }
        }
    }

}
