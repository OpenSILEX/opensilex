//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.bigdata;

import org.opensilex.bigdata.exceptions.BigDataTransactionException;
import org.opensilex.service.ServiceConnection;


public interface BigDataConnection extends ServiceConnection {

    public void startTransaction() throws BigDataTransactionException;

    public void commitTransaction() throws BigDataTransactionException;

    public void rollbackTransaction() throws BigDataTransactionException;
    
}
