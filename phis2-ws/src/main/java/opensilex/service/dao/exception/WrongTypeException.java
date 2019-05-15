//******************************************************************************
//                            WrongTypeException.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 5 Apr. 2019
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao.exception;

/**
 * Unknown URI exception.
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class WrongTypeException extends SemanticInconsistencyException {
    
    public final static String GENERIC_MESSAGE = "An URI is of the wrong type";
    public final static String DETAILS = "The URI %s of %s has not the right type";
    
    public WrongTypeException() {
        super(DETAILS);
    }
    
    public WrongTypeException(Throwable throwableCause) {
        super(DETAILS, throwableCause);
    }
    
    public WrongTypeException(String uriOfWhichTypeIsIncorrect, String objectConcerned) {
        super(String.format(DETAILS, uriOfWhichTypeIsIncorrect, objectConcerned));
    }
    
    public WrongTypeException(String uriOfWhichTypeIsIncorrect, String objectConcerned, Throwable throwableCause) {
        super(String.format(DETAILS, uriOfWhichTypeIsIncorrect, objectConcerned), throwableCause);
    }

    @Override
    public String getGenericMessage() {
        return GENERIC_MESSAGE;
    }
}
