//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.response;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author vincent
 */
public class ErrorResult {

    private final static Logger LOGGER = LoggerFactory.getLogger(ErrorResult.class);

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

    public ErrorResult(String title, String message) {
        this(title, message, null);
    }

    public ErrorResult(String title, String message, Throwable t) {
        this.title = title;
        this.message = message;

        if (t != null && LOGGER.isDebugEnabled()) {
            stack = new ArrayList<>();
            fullstack = new ArrayList<>();
            for (StackTraceElement stackElement : t.getStackTrace()) {
                String stackMessage = stackElement.toString();
                if (stackMessage.startsWith("org.opensilex")) {
                    stack.add(stackMessage);
                }
                fullstack.add(stackMessage);
            }
        }
    }
}
