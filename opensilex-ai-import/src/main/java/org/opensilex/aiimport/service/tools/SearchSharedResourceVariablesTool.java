//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensilex.aiimport.resolve.ResourceReference;
import org.opensilex.aiimport.resolve.SharedResourceVariableLookup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Searches the variables of the shared resource instances declared in the configuration.
 * <p>
 * Finding a variable there means the user can import it instead of creating one from scratch, which
 * is the outcome to steer towards: it keeps the instance aligned with the reference catalogue.
 *
 * @author Arnaud Charleroy
 */
public class SearchSharedResourceVariablesTool implements AiTool {

    public static final String NAME = "search_variables_in_shared_resource";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Search the variables of the shared resource instances this OpenSILEX instance is "
                + "configured with. Use it when search_variables found nothing locally: a variable "
                + "available on a shared resource instance should be imported from there rather "
                + "than created by hand.";
    }

    @Override
    public ObjectNode getParametersSchema(ObjectMapper mapper) {
        ObjectNode schema = ToolSchemas.object(mapper);
        ToolSchemas.string(schema, "name", "Exact variable name to look for", true);
        return schema;
    }

    @Override
    public Object execute(JsonNode arguments, ToolContext context) {
        String name = ToolSchemas.text(arguments, "name");
        if (name == null) {
            return ToolSchemas.error("The 'name' argument is required.");
        }

        SharedResourceVariableLookup lookup = context.getSharedResources();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("query", name);

        if (lookup == null || !lookup.isEnable()) {
            response.put("count", 0);
            response.put("variables", new ArrayList<>());
            response.put("note", "No shared resource instance is configured on this OpenSILEX instance.");
            return response;
        }

        List<String> warnings = new ArrayList<>();
        List<Map<String, Object>> results = new ArrayList<>();
        for (ResourceReference reference : lookup.search(name, warnings)) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("uri", String.valueOf(reference.getUri()));
            entry.put("name", reference.getName());
            entry.put("shared_resource_instance", reference.getSharedResourceInstance());
            entry.put("shared_resource_instance_label", reference.getSharedResourceInstanceLabel());
            results.add(entry);
        }

        response.put("count", results.size());
        response.put("variables", results);
        if (!warnings.isEmpty()) {
            response.put("warnings", warnings);
        }
        return response;
    }
}
