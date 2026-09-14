//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensilex.core.project.dal.ProjectDAO;
import org.opensilex.core.project.dal.ProjectModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Searches the projects of this instance by name or short name.
 *
 * @author Arnaud Charleroy
 */
public class SearchProjectsTool implements AiTool {

    public static final String NAME = "search_projects";

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 25;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Search the projects of this OpenSILEX instance by name or short name. Use it to find "
                + "which project an experiment could be attached to.";
    }

    @Override
    public ObjectNode getParametersSchema(ObjectMapper mapper) {
        ObjectNode schema = ToolSchemas.object(mapper);
        ToolSchemas.string(schema, "name", "Name or fragment of a name to search for", true);
        ToolSchemas.integer(schema, "limit", "Maximum number of results, at most " + MAX_LIMIT, false);
        return schema;
    }

    @Override
    public Object execute(JsonNode arguments, ToolContext context) throws Exception {
        String name = ToolSchemas.text(arguments, "name");
        if (name == null) {
            return SearchVariablesTool.error("The 'name' argument is required.");
        }
        int limit = ToolSchemas.number(arguments, "limit", DEFAULT_LIMIT, MAX_LIMIT);

        ProjectDAO dao = new ProjectDAO(context.getSparql());
        List<Map<String, Object>> results = new ArrayList<>();
        for (ProjectModel project : dao.search(name, null, null, null, context.getCurrentUser(),
                null, 0, limit).getList()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("uri", String.valueOf(project.getUri()));
            entry.put("name", project.getName());
            entry.put("shortname", project.getShortname());
            entry.put("start_date", String.valueOf(project.getStartDate()));
            entry.put("end_date", String.valueOf(project.getEndDate()));
            results.add(entry);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("query", name);
        response.put("count", results.size());
        response.put("projects", results);
        return response;
    }
}
