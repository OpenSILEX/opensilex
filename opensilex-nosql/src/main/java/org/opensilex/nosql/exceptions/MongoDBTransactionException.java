//******************************************************************************
//                     NoSQLTransactionException.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.exceptions;

import com.mongodb.MongoException;

/**
 * Base exception for big data service.
 * 
 * @author rcolin
 */
public class MongoDBTransactionException extends MongoException {

    private final Exception innerException;

    public MongoDBTransactionException(String msg, Exception t) {
        super(msg, t);
        this.innerException = t;
    }

    public Exception getInnerException() {
        return innerException;
    }
}
