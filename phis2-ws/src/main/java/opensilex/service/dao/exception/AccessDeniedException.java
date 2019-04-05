//******************************************************************************
//                                       AccessDeniedException.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 5 Apr. 2019
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao.exception;

/**
 *
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class AccessDeniedException extends Exception {
    public AccessDeniedException(String errorMessage) {
        super(errorMessage);
    }
}
