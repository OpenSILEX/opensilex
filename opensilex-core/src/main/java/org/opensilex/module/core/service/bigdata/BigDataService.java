package org.opensilex.module.core.service.bigdata;

import org.opensilex.service.Service;
import org.opensilex.module.core.service.bigdata.exceptions.BigDataTransactionException;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author vincent
 */
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
}
