//******************************************************************************
//                     DAODataErrorAggregateException.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 8 Apr 2019
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao.exception;

import java.util.ArrayList;

/**
 * Exception aggregator for data checks in DAOs. 
 * This exception aggregator does nothing special compared to its mother class. 
 * Its only purpose is to be used when multiple exceptions have to be thrown when 
 * checking data in DAOs. Thus, further in the process, the code that will analyse 
 * the exceptions will identify them as thrown when checking data, enabling it to
 * handle them particularly if needed.
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class DAODataErrorAggregateException extends AggregateException {

    public DAODataErrorAggregateException(ArrayList<DAODataErrorException> exceptions) {
        super(exceptions);
    }
    
    @Override
    public ArrayList<DAODataErrorException> getExceptions() {
        return (ArrayList<DAODataErrorException>) exceptions;
    }
}