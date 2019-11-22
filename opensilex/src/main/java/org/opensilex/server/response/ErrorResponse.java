//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.response;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.*;
import javax.ws.rs.core.Response.*;

/**
 * Internal class used to format the exception, removing useless information.
 */
@JsonInclude(Include.NON_NULL)
public class ErrorResponse extends JsonResponse<ErrorResult> {

    /**
     * Constructor
     *
     * @param exception Exception to wrap into a structured error response
     */
    public ErrorResponse(Throwable exception) {
        super(Status.INTERNAL_SERVER_ERROR);
        metadata = new Metadata(new Pagination());
        result = new ErrorResult(
                "Unexpected internal error - " + exception.getClass().getCanonicalName(),
                exception.getMessage(),
                exception
        );
    }

    public ErrorResponse(Status status, String title, String message) {
        super(status);
        metadata = new Metadata(new Pagination());
        result = new ErrorResult(title, message);
    }

}
