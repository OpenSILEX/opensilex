//******************************************************************************
//                        BigDataConnection.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.bigdata;

import org.opensilex.bigdata.exceptions.BigDataTransactionException;
import org.opensilex.service.ServiceConnection;

/**
 * Interface to describe big data connection required features.
 * <pre>
 * TODO: Only implement transaction for the moment, datanucleus integration
 * to achieve: http://www.datanucleus.org/
 * </pre>
 *
 * @see org.opensilex.bigdata.BigDataService
 * @author Vincent Migot
 */
public interface BigDataConnection extends ServiceConnection {

    /**
     * Start a transaction
     *
     * @throws BigDataTransactionException
     */
    public void startTransaction() throws BigDataTransactionException;

    /**
     * Commit a transaction
     *
     * @throws BigDataTransactionException
     */
    public void commitTransaction() throws BigDataTransactionException;

    /**
     * Rollback a transaction
     *
     * @throws BigDataTransactionException
     */
    public void rollbackTransaction() throws BigDataTransactionException;

}
