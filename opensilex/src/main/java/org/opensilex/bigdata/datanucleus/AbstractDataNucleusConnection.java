//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.bigdata.datanucleus;

import java.util.*;
import org.opensilex.bigdata.*;
import org.opensilex.bigdata.exceptions.*;


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
