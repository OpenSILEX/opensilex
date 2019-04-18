//******************************************************************************
//                          GenericExceptionMapper.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 1 Apr. 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.request.filter;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Handles generic exceptions in web services and displays them as JSON.
 * @author Vincent Migot <vincent.migot@inra.fr>
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    /**
     * Converts the exception to JSON
     * @param exception
     * @return JSON error response
     */
    @Override
    public Response toResponse(Throwable exception) {

        return Response.status(500)
                .entity(new ErrorMessage(exception))
                .type(MediaType.APPLICATION_JSON).
                build();
    }

    /**
     * Internal class used to format the exception, removing useless information.
     */
    private class ErrorMessage {

        /**
         * Title field of the exception containing the class name.
         */
        private final String title;
        
        /**
         * Message of the exception.
         */
        private final String message;
        
        /**
         * Stack trace of the exception as an array.
         */
        private final List<String> stack = new ArrayList<>();

        /**
         * Constructor
         * @param exception 
         */
        private ErrorMessage(Throwable exception) {
            this.title = "Unexpected internal error - " + exception.getClass().getCanonicalName();
            this.message = exception.getMessage();

            for (StackTraceElement stackElement : exception.getStackTrace()) {
                stack.add(stackElement.toString());
            }
        }
    }
}
