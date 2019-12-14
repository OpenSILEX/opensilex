//******************************************************************************
//                         BigDataService.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.bigdata;

import org.opensilex.bigdata.exceptions.BigDataTransactionException;
import org.opensilex.bigdata.mongodb.MongoDBConfig;
import org.opensilex.bigdata.mongodb.MongoDBConnection;
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
public class BigDataService implements BigDataConnection, Service {

    /**
     * Constructor with BigDataConnection to allow multiple configurable
     * implementations
     *
     * @param connection Connection for the service
     */
    public BigDataService(BigDataConnection connection) {
        this.connection = connection;
    }

    private final BigDataConnection connection;

    @Override
    public void startTransaction() throws BigDataTransactionException {
        connection.startTransaction();
    }

    @Override
    public void commitTransaction() throws BigDataTransactionException {
        connection.commitTransaction();
    }

    @Override
    public void rollbackTransaction() throws BigDataTransactionException {
        connection.rollbackTransaction();
    }

    @Override
    public void startup() {
        connection.startup();
    }

    @Override
    public void shutdown() {
        connection.shutdown();
    }
}
