//******************************************************************************
//                            AggregateException.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 8 Apr 2019
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao.exception;

import java.util.ArrayList;

/**
 * Exception aggregator. Permits to handle multiple exceptions at once, for example 
 * when we don't want a function to stop when the first exception is thrown.
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class AggregateException extends Exception {

    protected final ArrayList<? extends Exception> exceptions;

    public AggregateException(ArrayList<? extends Exception> exceptions) {
        super();
        this.exceptions = exceptions;
    }

    public ArrayList<? extends Exception> getExceptions() {
        return exceptions;
    }
}