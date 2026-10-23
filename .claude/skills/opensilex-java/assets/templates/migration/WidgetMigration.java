package org.opensilex.migration;

import org.opensilex.sparql.SPARQLConfig;
import org.opensilex.sparql.service.SPARQLService;

import java.time.OffsetDateTime;

/**
 * Describe in one sentence what this migration changes in the stored data.
 * <p>
 * Use a migration when a code change alters stored data (new mandatory property, renamed predicate, moved graph, Mongo
 * collection change). It must be safe to run twice and on empty data: check the precondition and return early when
 * there is nothing to migrate. Run it with
 * {@code java -jar opensilex.jar system run-update org.opensilex.migration.WidgetMigration}.
 */
public class WidgetMigration extends DatabaseMigrationModuleUpdate {

    /**
     * The interface documents this as the creation date of the update; the inherited default is {@code now()} and the
     * runner only logs it. A fixed date keeps the log meaningful.
     */
    @Override
    public OffsetDateTime getDate() {
        return OffsetDateTime.parse("2026-10-09T00:00:00+02:00");
    }

    @Override
    public String getDescription() {
        return "Describe the data change here";
    }

    @Override
    protected boolean applyOnSparql(SPARQLService sparql, SPARQLConfig sparqlConfig) {
        return true;
    }

    @Override
    protected void sparqlOperation(SPARQLService sparql, SPARQLConfig sparqlConfig) throws Exception {
        logger.info("Starting: {}", getDescription());

        // 1. Return early when there is nothing to migrate.
        // 2. Wrap multi-step writes in sparql.startTransaction() / commitTransaction() / rollbackTransaction(e),
        //    and rethrow: a migration that logs and swallows its failure leaves the database half-migrated.
        // 3. Log the number of migrated resources at INFO so that an administrator can verify the run.
    }
}
