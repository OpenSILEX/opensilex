package org.opensilex.module.core.service.nosql;

import org.opensilex.service.Service;
import org.opensilex.module.core.service.nosql.exceptions.NoSQLTransactionException;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author vincent
 */
public class NoSQLService implements NoSQLConnection, Service {
    
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
}
