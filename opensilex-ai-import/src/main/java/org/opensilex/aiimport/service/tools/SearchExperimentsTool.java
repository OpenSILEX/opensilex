//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.dal.ExperimentSearchFilter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Searches the experiments the current account may see.
 *
 * @author Arnaud Charleroy
 */
public class SearchExperimentsTool implements AiTool {

    public static final String NAME = "search_experiments";

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 25;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Search the experiments of this OpenSILEX instance by name. Only the experiments the "
                + "current user may see are returned. Use it to find which experiment a trial name "
                + "in the file corresponds to.";
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

        ExperimentDAO dao = new ExperimentDAO(context.getSparql(), context.getNosql(), context.getFs());
        ExperimentSearchFilter filter = new ExperimentSearchFilter()
                .setName(name)
                .setUser(context.getCurrentUser());
        filter.setLang(context.getCurrentUser().getLanguage());
        filter.setPage(0);
        filter.setPageSize(limit);

        List<Map<String, Object>> results = new ArrayList<>();
        for (ExperimentModel experiment : dao.search(filter, false, false, false).getList()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("uri", String.valueOf(experiment.getUri()));
            entry.put("name", experiment.getName());
            entry.put("start_date", String.valueOf(experiment.getStartDate()));
            entry.put("end_date", String.valueOf(experiment.getEndDate()));
            results.add(entry);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("query", name);
        response.put("count", results.size());
        response.put("experiments", results);
        return response;
    }
}
