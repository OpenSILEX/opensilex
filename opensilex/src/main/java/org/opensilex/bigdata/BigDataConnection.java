/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.bigdata;

import org.opensilex.service.ServiceConnection;
import org.opensilex.bigdata.exceptions.BigDataTransactionException;

/**
 *
 * @author Vincent Migot
 */
public interface BigDataConnection extends ServiceConnection {

    public void startTransaction() throws BigDataTransactionException;

    public void commitTransaction() throws BigDataTransactionException;

    public void rollbackTransaction() throws BigDataTransactionException;
    
}
