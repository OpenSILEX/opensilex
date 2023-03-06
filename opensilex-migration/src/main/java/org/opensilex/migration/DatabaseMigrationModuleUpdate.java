package org.opensilex.migration;

import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.SPARQLConfig;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.utils.AnsiUtils;
import org.opensilex.update.AbstractOpenSilexModuleUpdate;
import org.opensilex.update.OpensilexModuleUpdateException;

/**
 * An abstract implementation of Module update which handle Sparql/MongoDB operation by handling :
 * <ul>
 *     <li>Service and Config instantiation (for SPARQL and MongoDB)</li>
 *     <li>Transaction handling (start, commit and rollback)</li>
 *     <li>Verification of applicability of database operations</li>
 * </ul>
 *
 * @author rcolin
 */
public abstract class DatabaseMigrationModuleUpdate extends AbstractOpenSilexModuleUpdate {

    protected SPARQLService getSparql() {
        SPARQLServiceFactory factory = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        return factory.provide();
    }

    protected SPARQLConfig getSparqlConfig() {
        try {
            return opensilex.getModuleConfig(SPARQLModule.class, SPARQLConfig.class);
        } catch (OpenSilexModuleNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    protected MongoDBService getMongoDB() {
        return opensilex.getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);
    }

    protected MongoDBConfig getMongoConfig() {
        return opensilex.loadConfigPath(MongoDBConfig.DEFAULT_CONFIG_PATH, MongoDBConfig.class);
    }

    protected boolean applyOnSparql(SPARQLService sparql, SPARQLConfig sparqlConfig) throws SPARQLException {
        return false;
    }
    protected boolean applyOnMongodb(MongoDBService mongo, MongoDBConfig mongoDBConfig){
        return false;
    }

    protected void sparqlOperation(SPARQLService sparql, SPARQLConfig sparqlConfig) throws SPARQLException {

    }

    protected void mongoOperation(MongoDBService mongo, MongoDBConfig mongoDBConfig) {

    }

    @Override
    public final void execute() throws OpensilexModuleUpdateException {

        SPARQLService sparql = getSparql();
        SPARQLConfig sparqlConfig = getSparqlConfig();

        // determine if an Operation on the triple store is needed or not
        // the operation can fail, in case where some interaction with the database is needed to check if migration apply or not
        final boolean applyOnSparql;
        try {
            applyOnSparql = applyOnSparql(sparql, sparqlConfig);
        } catch (SPARQLException e) {
            throw new RuntimeException(e);
        }

        MongoDBService mongo = getMongoDB();
        MongoDBConfig mongoConfig = getMongoConfig();
        boolean applyOnMongo = applyOnMongodb(mongo, mongoConfig);

        try {
            if (applyOnSparql) {
                sparql.startTransaction();
                AnsiUtils.logDebug(logger,"Running migration in RDF4J Triple Store");
                sparqlOperation(sparql, sparqlConfig);
            }

            if (applyOnMongo) {
                mongo.startTransaction();
                AnsiUtils.logDebug(logger,"Running migration in MongoDB document store");
                mongoOperation(mongo, mongoConfig);
            }

            if (applyOnSparql) {
                sparql.commitTransaction();
            }
            if (applyOnMongo) {
                mongo.commitTransaction();
            }

        } catch (Exception e) {
            try {
                if (applyOnSparql) {
                    sparql.rollbackTransaction();
                }
                if(applyOnMongo){
                    mongo.rollbackTransaction();
                }
                throw new OpensilexModuleUpdateException(this, e);
            } catch (Exception e2) {
                throw new OpensilexModuleUpdateException(this, e2);
            }
        }


    }
}
