/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.service.bigdata;

import org.opensilex.service.ServiceConnection;
import org.opensilex.module.core.service.bigdata.exceptions.BigDataTransactionException;

/**
 *
 * @author vincent
 */
public interface BigDataConnection extends ServiceConnection {

    public void startTransaction() throws BigDataTransactionException;

    public void commitTransaction() throws BigDataTransactionException;

    public void rollbackTransaction() throws BigDataTransactionException;
    
}
