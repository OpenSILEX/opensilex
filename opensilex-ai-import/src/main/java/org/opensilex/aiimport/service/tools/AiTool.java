//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A read-only lookup the assistant may perform on the user's behalf.
 * <p>
 * Every tool is a query, never a write. This is what lets the assistant state that a variable
 * exists without inventing it: the URIs it cites come back from here.
 *
 * @author Arnaud Charleroy
 */
public interface AiTool {

    String getName();

    /**
     * @return what the tool does and when to call it, written for the model
     */
    String getDescription();

    /**
     * @return a JSON Schema object describing the accepted arguments
     */
    ObjectNode getParametersSchema(ObjectMapper mapper);

    /**
     * @param arguments the arguments the model provided, already parsed
     * @return any object; it is serialised to JSON and handed back to the model
     */
    Object execute(JsonNode arguments, ToolContext context) throws Exception;
}
