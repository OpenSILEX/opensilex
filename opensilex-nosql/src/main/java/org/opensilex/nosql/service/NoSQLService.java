//******************************************************************************
//                         NoSQLService.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.service;

import org.opensilex.nosql.exceptions.NoSQLTransactionException;
import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.nosql.mongodb.MongoDBConnection;
import org.opensilex.service.Service;
import org.opensilex.service.ServiceConfigDefault;

/**
 * Service for big data access and storage.
 * <pre>
 * TODO: Only implement transaction for the moment, datanucleus integration
 * to achieve: http://www.datanucleus.org/
 * </pre>
 *
 * @author Vincent Migot
 */
@ServiceConfigDefault(
        connection = MongoDBConnection.class,
        connectionConfig = MongoDBConfig.class,
        connectionConfigID = "mongodb"
)
public class NoSQLService implements NoSQLConnection, Service {

    /**
     * Constructor with NoSQLConnection to allow multiple configurable
 implementations
     *
     * @param connection Connection for the service
     */
    public NoSQLService(NoSQLConnection connection) {
        this.connection = connection;
    }

    private final NoSQLConnection connection;

    @Override
    public void startTransaction() throws NoSQLTransactionException {
        connection.startTransaction();
    }

    @Override
    public void commitTransaction() throws NoSQLTransactionException {
        connection.commitTransaction();
    }

    @Override
    public void rollbackTransaction() throws NoSQLTransactionException {
        connection.rollbackTransaction();
    }

    @Override
    public void startup() throws Exception {
        connection.startup();
    }

    @Override
    public void shutdown() throws Exception {
        connection.shutdown();
    }
}
