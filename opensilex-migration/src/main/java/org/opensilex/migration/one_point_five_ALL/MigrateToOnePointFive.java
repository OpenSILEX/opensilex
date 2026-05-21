package org.opensilex.migration.one_point_five_ALL;

import org.opensilex.OpenSilex;
import org.opensilex.nosql.distributed.SparqlMongoTransaction;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.update.OpenSilexModuleUpdate;
import org.opensilex.update.OpensilexModuleUpdateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;

/**
 * A single migration to upgrade from 1.4.* to 1.5.0, this includes the following, see descriptions for more details:
 * - FacilitiesLinkToVariablesAndDevicesMigration
 * - UpdateScientificObjectsAndMovesWithLocationObservationCollectionModel
 * - ScientificObjectAndExperimentRelationMigration
 * - UpdateFacilitiesWithLocationObservationCollectionModel
 */
public class MigrateToOnePointFive implements OpenSilexModuleUpdate {

    private OpenSilex opensilex;

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
                UpdateFacilitiesWithLocationObservationCollectionModel.DESCRIPTION;
    }

    @Override
    public void execute() throws OpensilexModuleUpdateException {
        //Initialize services
        SPARQLServiceFactory factory = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();
        MongoDBService mongodb = opensilex.getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);
        Logger logger = LoggerFactory.getLogger(getClass());
        //Initialize the sub-part migration classes
        var facilitiesLinkToVariablesAndDevicesMigration = new FacilitiesLinkToVariablesAndDevicesMigration(sparql, mongodb, logger);
        var sciObjsAndMovesLocationMigration = new UpdateScientificObjectsAndMovesWithLocationObservationCollectionModel(sparql, mongodb, logger);
        var sciObjAndXpLinkMigration = new ScientificObjectAndExperimentRelationMigration(sparql, logger);
        var facilitiesLocationsMigration = new UpdateFacilitiesWithLocationObservationCollectionModel(sparql, mongodb, logger);
        var germplasmAttributeUpdateRights = new GermplasmAttributeUpdateRightsMigration();
        //Check migration has not already been run by checking each sub-migration
        try{
            if(
                    facilitiesLinkToVariablesAndDevicesMigration.wasMigrationPreviouslyRun() ||
                            sciObjsAndMovesLocationMigration.wasMigrationPreviouslyRun() ||
                            sciObjAndXpLinkMigration.wasMigrationPreviouslyRun() ||
                            facilitiesLocationsMigration.wasMigrationPreviouslyRun()
            ){
                logger.warn("It looks like this migration has already been performed. If this is not the case contact the Opensilex Team! No changes saved on the database.");
                return;
            }
        }catch(Exception e){
            logger.error(" No changes saved on the database. Something went wrong during verification that migration was not already run, the error was: {}", e.getMessage());
            return;
        }
        //Execute them
        try{

            new SparqlMongoTransaction(sparql, mongodb.getServiceV2()).execute(session ->{
                facilitiesLinkToVariablesAndDevicesMigration.execute();
                sciObjsAndMovesLocationMigration.execute(session);
                sciObjAndXpLinkMigration.execute();
                facilitiesLocationsMigration.execute(session);
                germplasmAttributeUpdateRights.executeWithoutTransaction(sparql, mongodb.getServiceV2());
                return null;
            });

            logger.info("Migration successfully completed");
        }catch(Exception e){
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
