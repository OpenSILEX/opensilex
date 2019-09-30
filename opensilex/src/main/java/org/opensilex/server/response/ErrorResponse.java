/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal class used to format the exception, removing useless information.
 */
@JsonInclude(Include.NON_NULL)
public class ErrorResponse extends JsonResponse {

    private final static Logger LOGGER = LoggerFactory.getLogger(ErrorResponse.class);

    /**
     * Title field of the exception containing the class name.
     */
    public final String title;

    /**
     * Message of the exception.
     */
    public final String message;

    /**
     * Stack trace of the exception as an array.
     */
    public List<String> stack;

    /**
     * Stack trace of the exception as an array.
     */
    public List<String> fullstack;

    /**
     * Constructor
     *
     * @param exception Exception to wrap into a structured error response
     */
    public ErrorResponse(Throwable exception) {
        this(
                "Unexpected internal error - " + exception.getClass().getCanonicalName(),
                exception.getMessage()
        );

        if (LOGGER.isDebugEnabled()) {
            stack = new ArrayList<>();
            fullstack = new ArrayList<>();
            for (StackTraceElement stackElement : exception.getStackTrace()) {
                String stackMessage = stackElement.toString();
                if (stackMessage.startsWith("org.opensilex")) {
                    stack.add(stackMessage);
                }
                fullstack.add(stackMessage);
            }
        }
    }

    public ErrorResponse(String title, String message) {
        this(Status.INTERNAL_SERVER_ERROR, title, message);
    }

    public ErrorResponse(Status status, String title, String message) {
        super(status);
        this.title = title;
        this.message = message;
    }

}
