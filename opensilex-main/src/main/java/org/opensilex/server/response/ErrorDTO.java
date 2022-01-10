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
import java.util.Map;

/**
 * Error DTO class.
 * <pre>
 * This class define an error DTO used by {@code org.opensilex.server.response.ErrorResponse}
 * It's defined by a title and a message and eventually a source exception.
 * A translation key (and values) can also be provided if the error should be displayed to the user.
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
     * Translation key of the error, used to display a message for the user. Can be null.
     */
    @ApiModelProperty(hidden = true)
    public final String translationKey;

    /**
     * Translation values of the error, used to fill parameterized values in the translated message for the user. Can be null.
     */
    @ApiModelProperty(hidden = true)
    public final Map<String, String> translationValues;

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
    public ErrorDTO(String title, String message, String translationKey, Map<String, String> translationValues, Throwable t) {
        this.title = title;
        this.message = message;
        this.translationKey = translationKey;
        this.translationValues = translationValues;

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

    public ErrorDTO(String title, String message, Throwable t) {
        this(title, message, null, null, t);
    }
}
