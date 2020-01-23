//******************************************************************************
//                     NoSQLTransactionException.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.exceptions;

/**
 * Base exception for big data service.
 * <pre>
 * TODO: Not really implemented yet !
 * </pre>
 * 
 * @see org.opensilex.nosql.service.NoSQLService
 * @author Vincent Migot
 */
public class NoSQLTransactionException extends Exception {

    /**
     * Constructor to encapsulate other exceptions
     * 
     * @param ex Exception to encapsulate
     */
    public NoSQLTransactionException(Exception ex) {
        super(ex);
    }

}
