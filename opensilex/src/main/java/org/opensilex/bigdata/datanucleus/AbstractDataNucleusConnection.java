/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.bigdata.datanucleus;

import java.util.Map;
import org.opensilex.bigdata.exceptions.BigDataTransactionException;
import org.opensilex.bigdata.BigDataConnection;

/**
 *
 * @author Vincent Migot
 */
public abstract class AbstractDataNucleusConnection implements BigDataConnection {

    public AbstractDataNucleusConnection(Map<?, ?> props) {
//        PersistenceManagerFactory manager = JDOHelper.getPersistenceManagerFactory(props);
//        transactionManager = com.arjuna.ats.jta.TransactionManager.transactionManager();
    }

    @Override
    public void startTransaction() throws BigDataTransactionException {

    }

    @Override
    public void commitTransaction() throws BigDataTransactionException {

    }

    @Override
    public void rollbackTransaction() throws BigDataTransactionException {

    }
}
