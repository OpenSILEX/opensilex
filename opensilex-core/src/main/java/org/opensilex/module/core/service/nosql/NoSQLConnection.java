/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.service.nosql;

import org.opensilex.service.ServiceConnection;
import org.opensilex.module.core.service.nosql.exceptions.NoSQLTransactionException;

/**
 *
 * @author vincent
 */
public interface NoSQLConnection extends ServiceConnection {

    public void startTransaction() throws NoSQLTransactionException;

    public void commitTransaction() throws NoSQLTransactionException;

    public void rollbackTransaction() throws NoSQLTransactionException;
}
