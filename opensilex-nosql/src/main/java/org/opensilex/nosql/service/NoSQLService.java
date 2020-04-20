//******************************************************************************
//                         NoSQLService.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.service;

import org.opensilex.nosql.exceptions.NoSQLTransactionException;
import org.opensilex.nosql.mongodb.MongoDBConnection;
import org.opensilex.service.BaseService;
import org.opensilex.service.Service;
import org.opensilex.service.ServiceDefaultDefinition;

/**
 * Service for big data access and storage.
 * <pre>
 * TODO: Only implement transaction for the moment, datanucleus integration
 * to achieve: http://www.datanucleus.org/
 * </pre>
 *
 * @author Vincent Migot
 */
@ServiceDefaultDefinition(
        serviceClass = MongoDBConnection.class,
        serviceID = "mongodb"
)
public class NoSQLService extends BaseService implements NoSQLConnection, Service {

    /**
     * Constructor with NoSQLConnection to allow multiple configurable
     * implementations
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
    public void rollbackTransaction(Exception ex) throws Exception {
        connection.rollbackTransaction(ex);
    }

    @Override
    public void setup() throws Exception {
        connection.setOpenSilex(getOpenSilex());
        connection.setup();
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
