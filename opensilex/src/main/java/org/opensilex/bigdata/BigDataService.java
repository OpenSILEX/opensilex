//******************************************************************************
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Vincent Migot
 */
@ServiceConfigDefault(
        connection = MongoDBConnection.class,
        connectionConfig = MongoDBConfig.class,
        connectionConfigID = "mongodb"
)
public class BigDataService implements BigDataConnection, Service {

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
