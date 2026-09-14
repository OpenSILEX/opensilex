//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.core.variable.dal.VariableSearchFilter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Searches the variables of this instance by name.
 *
 * @author Arnaud Charleroy
 */
public class SearchVariablesTool implements AiTool {

    public static final String NAME = "search_variables";

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 25;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Search the variables of this OpenSILEX instance. The name is matched as a regular "
                + "expression against the variable name, its alternative name, and the names of its "
                + "entity, characteristic, method and unit. Use it to check whether a column of the "
                + "file already has a variable, and to read that variable's unit and data type.";
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
            return error("The 'name' argument is required.");
        }
        int limit = ToolSchemas.number(arguments, "limit", DEFAULT_LIMIT, MAX_LIMIT);

        VariableDAO dao = new VariableDAO(context.getSparql(), context.getNosql(), context.getFs(),
                context.getCurrentUser());
        VariableSearchFilter filter = new VariableSearchFilter()
                .setNamePattern(name)
                .setUserModel(context.getCurrentUser());
        filter.setLang(context.getCurrentUser().getLanguage());
        filter.setPage(0);
        filter.setPageSize(limit);

        List<Map<String, Object>> results = new ArrayList<>();
        for (VariableModel variable : dao.search(filter).getList()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("uri", String.valueOf(variable.getUri()));
            entry.put("name", variable.getName());
            entry.put("alternative_name", variable.getAlternativeName());
            entry.put("unit", variable.getUnit() == null ? null : variable.getUnit().getName());
            entry.put("method", variable.getMethod() == null ? null : variable.getMethod().getName());
            entry.put("datatype", variable.getDataType() == null ? null : String.valueOf(variable.getDataType()));
            results.add(entry);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("query", name);
        response.put("count", results.size());
        response.put("variables", results);
        return response;
    }

    static Map<String, Object> error(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error", message);
        return response;
    }
}
