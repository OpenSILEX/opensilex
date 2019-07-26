/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.service.bigdata.datanucleus;

import java.util.Map;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;
import org.opensilex.module.core.service.bigdata.exceptions.BigDataTransactionException;
import org.opensilex.module.core.service.bigdata.BigDataConnection;

/**
 *
 * @author vincent
 */
public abstract class AbstractDataNucleusConnection implements BigDataConnection {

    public AbstractDataNucleusConnection(Map<?, ?> props) {
        PersistenceManagerFactory manager = JDOHelper.getPersistenceManagerFactory(props);
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
