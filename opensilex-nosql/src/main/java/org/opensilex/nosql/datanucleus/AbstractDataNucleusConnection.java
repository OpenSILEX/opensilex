//******************************************************************************
//                   AbstractDataNucleusConnection.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.datanucleus;

import java.util.Map;
import org.opensilex.nosql.exceptions.NoSQLTransactionException;
import org.opensilex.nosql.service.NoSQLConnection;

/**
 * Datanucleus connection implementation.
 * <pre>
 * TODO: Implement it
 * </pre>
 *
 * @see org.opensilex.nosql.service.NoSQLConnection
 * @author Vincent Migot
 */
public abstract class AbstractDataNucleusConnection implements NoSQLConnection {

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
    public void startTransaction() throws NoSQLTransactionException {

    }

    @Override
    public void commitTransaction() throws NoSQLTransactionException {

    }

    @Override
    public void rollbackTransaction(Exception ex) throws Exception {
        if (ex != null) {
            throw ex;
        }
    }
}
