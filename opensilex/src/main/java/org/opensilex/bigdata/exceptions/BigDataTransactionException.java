//******************************************************************************
//                     BigDataTransactionException.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.bigdata.exceptions;

/**
 * Base exception for big data service.
 * <pre>
 * TODO: Not really implemented yet !
 * </pre>
 * 
 * @see org.opensilex.bigdata.BigDataService
 * @author Vincent Migot
 */
public class BigDataTransactionException extends Exception {

    /**
     * Constructor to encapsulate other exceptions
     * 
     * @param ex Exception to encapsulate
     */
    public BigDataTransactionException(Exception ex) {
        super(ex);
    }

}
