//******************************************************************************
//                   AbstractDataNucleusConnection.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.bigdata.datanucleus;

import java.util.Map;
import org.opensilex.bigdata.BigDataConnection;
import org.opensilex.bigdata.exceptions.BigDataTransactionException;

/**
 * Datanucleus connection implementation.
 * <pre>
 * TODO: Implement it
 * </pre>
 *
 * @see org.opensilex.bigdata.BigDataConnection
 * @author Vincent Migot
 */
public abstract class AbstractDataNucleusConnection implements BigDataConnection {

    /**
     * Constructor for datanucleus allowing any Map of properties for
     * configuration depending of concrete implementation requirements.
     *
     * @param props Map of properties to configure Datanucleus
     */
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
