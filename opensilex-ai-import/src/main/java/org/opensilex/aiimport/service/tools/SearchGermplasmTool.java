//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensilex.core.germplasm.api.GermplasmSearchFilter;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.germplasm.dal.GermplasmModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Searches the germplasm of this instance by name.
 *
 * @author Arnaud Charleroy
 */
public class SearchGermplasmTool implements AiTool {

    public static final String NAME = "search_germplasm";

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 25;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Search the germplasm of this OpenSILEX instance by name. Use it to check whether a "
                + "genotype written in the file is already registered, and under which exact name.";
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
            return ToolSchemas.error("The 'name' argument is required.");
        }
        int limit = ToolSchemas.number(arguments, "limit", DEFAULT_LIMIT, MAX_LIMIT);

        GermplasmDAO dao = new GermplasmDAO(context.getSparql(), context.getNosql());
        GermplasmSearchFilter filter = new GermplasmSearchFilter();
        filter.setName(name);
        filter.setUser(context.getCurrentUser());
        filter.setLang(context.getCurrentUser().getLanguage());
        filter.setPage(0);
        filter.setPageSize(limit);

        List<Map<String, Object>> results = new ArrayList<>();
        for (GermplasmModel germplasm : dao.search(filter, false, false).getList()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("uri", String.valueOf(germplasm.getUri()));
            entry.put("name", germplasm.getName());
            entry.put("code", germplasm.getCode());
            entry.put("rdf_type", String.valueOf(germplasm.getType()));
            results.add(entry);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("query", name);
        response.put("count", results.size());
        response.put("germplasm", results);
        return response;
    }
}
