package org.opensilex.migration.one_point_five_ALL;

import org.apache.commons.lang3.time.StopWatch;
import org.opensilex.OpenSilex;
import org.opensilex.nosql.distributed.SparqlMongoTransaction;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.update.OpenSilexModuleUpdate;
import org.opensilex.update.OpensilexModuleUpdateException;
import org.slf4j.Logger;

import java.time.OffsetDateTime;

import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A single migration to upgrade from 1.4.* to 1.5.0, this includes the following, see descriptions for more details:
 * - FacilitiesLinkToVariablesAndDevicesMigration
 * - UpdateScientificObjectsAndMovesWithLocationObservationCollectionModel
 * - ScientificObjectAndExperimentRelationMigration
 * - UpdateFacilitiesWithLocationObservationCollectionModel
 */
public class MigrateToOnePointFive implements OpenSilexModuleUpdate {

    private OpenSilex opensilex;
    public static final String LOGGER_FORMAT = "%s [%s/%s]";

    @Override
    public OffsetDateTime getDate() {
        return OffsetDateTime.now();
    }

    @Override
    public String getDescription() {
        return "A migration to pass from any 1.4 version to 1.5.0. This includes the following operations: \n" +
                FacilitiesLinkToVariablesAndDevicesMigration.DESCRIPTION +
                UpdateScientificObjectsAndMovesWithLocationObservationCollectionModel.DESCRIPTION +
                ScientificObjectAndExperimentRelationMigration.DESCRIPTION +
                UpdateFacilitiesWithLocationObservationCollectionModel.DESCRIPTION +
                ChangeTypeParametersUri.DESCRIPTION;
    }

    @Override
    public void execute() throws OpensilexModuleUpdateException {

        //Initialize services
        SPARQLServiceFactory factory = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();
        MongoDBService mongodb = opensilex.getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);
        Logger logger = getLogger(MigrateToOnePointFive.class);
        //Initialize the sub-part migration classes
        var facilitiesLinkToVariablesAndDevicesMigration = new FacilitiesLinkToVariablesAndDevicesMigration(sparql, mongodb,
                getLogger(format(LOGGER_FORMAT, FacilitiesLinkToVariablesAndDevicesMigration.class.getName(), 1, 6)));
        var sciObjsAndMovesLocationMigration = new UpdateScientificObjectsAndMovesWithLocationObservationCollectionModel(sparql, mongodb,
                getLogger(format(LOGGER_FORMAT, UpdateScientificObjectsAndMovesWithLocationObservationCollectionModel.class.getName(), 2, 6)));
        var sciObjAndXpLinkMigration = new ScientificObjectAndExperimentRelationMigration(sparql,
                getLogger(format(LOGGER_FORMAT, ScientificObjectAndExperimentRelationMigration.class.getName(), 3, 6)));
        var facilitiesLocationsMigration = new UpdateFacilitiesWithLocationObservationCollectionModel(sparql, mongodb,
                getLogger(format(LOGGER_FORMAT, UpdateFacilitiesWithLocationObservationCollectionModel.class.getName(), 4, 6)));
        var germplasmAttributeUpdateRights = new GermplasmAttributeUpdateRightsMigration(
                getLogger(format(LOGGER_FORMAT, GermplasmAttributeUpdateRightsMigration.class.getName(), 5, 6)));
        var changeTypeParametersUri = new ChangeTypeParametersUri(
                getLogger(format(LOGGER_FORMAT, ChangeTypeParametersUri.class.getName(), 6, 6)));
        changeTypeParametersUri.setSparql(sparql);
        //Check migration has not already been run by checking each sub-migration
        try {
            if (
                    facilitiesLinkToVariablesAndDevicesMigration.wasMigrationPreviouslyRun() ||
                            sciObjsAndMovesLocationMigration.wasMigrationPreviouslyRun() ||
                            sciObjAndXpLinkMigration.wasMigrationPreviouslyRun() ||
                            facilitiesLocationsMigration.wasMigrationPreviouslyRun()
            ) {
                logger.warn("It looks like this migration has already been performed. If this is not the case contact the Opensilex Team! No changes saved on the database.");
                return;
            }
        } catch (Exception e) {
            logger.error(" No changes saved on the database. Something went wrong during verification that migration was not already run, the error was: {}", e.getMessage());
            return;
        }
        //Execute them
        try {

            var watch = new StopWatch();
            new SparqlMongoTransaction(sparql, mongodb.getServiceV2()).execute(session -> {
                watch.start();
                facilitiesLinkToVariablesAndDevicesMigration.execute();
                sciObjsAndMovesLocationMigration.execute(session);
                sciObjAndXpLinkMigration.execute();
                facilitiesLocationsMigration.execute(session);
                germplasmAttributeUpdateRights.executeWithSession(sparql, mongodb.getServiceV2(), session);
                changeTypeParametersUri.execute();
                watch.stop();
                logger.debug(format("Total time : %.1fs", (float) watch.getTime() / 1000.f));
                return null;
            });

            logger.info("Migration successfully completed");
        } catch (Exception e) {
            try {
                logger.error("Error while migrating to 1.5. No changes were saved on the databases", e);
            } catch (Exception exception) {
                throw new OpensilexModuleUpdateException("Error while migrating to 1.5. No changes were saved on the databases", exception);
            }
        }

    }


    @Override
    public void setOpensilex(OpenSilex opensilex) {
        this.opensilex = opensilex;
    }
}
