//******************************************************************************
//                           ErrorDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Error DTO class.
 * <pre>
 * This class define an error DTO used by {@code org.opensilex.server.response.ErrorResponse}
 * It's defined by a title and a message and eventually a source exception.
 *
 * ONLY IN DEBUG MODE:
 * If error result is constructed with an exception result will contains two version of the stack trace arrays
 * - "stack" contains exception stack trace array filtered by packages org.opensilex.*
 * - "fullstack" contains complete exception stack trace array
 * </pre>
 *
 * @see org.opensilex.server.response.ErrorResponse
 * @author Vincent Migot
 */
@ApiModel
public class ErrorDTO {

    /**
     * Title of the error.
     */
    @ApiModelProperty(value = "Title of the error", example = "Error")
    public final String title;

    /**
     * Message of the error.
     */
    @ApiModelProperty(value = "Message of the error", example = "Unexpected error")
    public final String message;

    /**
     * Stack trace of the exception as an array.
     */
    @ApiModelProperty(hidden = true)
    public List<String> stack;

    /**
     * Stack trace of the exception as an array.
     */
    @ApiModelProperty(hidden = true)
    public List<String> fullstack;

    public ErrorDTO(){
        this(null,null);
    }

    /**
     * Error DTO constructor.
     *
     * @param title error title
     * @param message error message
     */
    public ErrorDTO(String title, String message) {
        this(title, message, null);
    }

    /**
     * Error DTO constructor.
     *
     * @param title error title
     * @param message error message
     * @param t errror cause
     */
    public ErrorDTO(String title, String message, Throwable t) {
        this.title = title;
        this.message = message;

        if (t != null) {
            StackTraceElement[] trace = t.getStackTrace();
            stack = new ArrayList<>(trace.length);
            fullstack = new ArrayList<>(trace.length);
            for (StackTraceElement stackElement : trace) {
                String stackMessage = stackElement.toString();
                if (stackMessage.startsWith("org.opensilex")) {
                    stack.add(stackMessage);
                }
                fullstack.add(stackMessage);
            }
        }
    }
}
