//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensilex.aiimport.resolve.ResolutionReport;
import org.opensilex.aiimport.resolve.ResolutionStatus;
import org.opensilex.aiimport.resolve.ResolvedItem;
import org.opensilex.aiimport.resolve.ResourceReference;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Returns one category of the validation report in full.
 * <p>
 * The prompt carries counts and a handful of examples per status. A file with hundreds of plots
 * would otherwise spend most of the context repeating the same line, and a question about one
 * category does not need the other four.
 *
 * @author Arnaud Charleroy
 */
public class GetReportTool implements AiTool {

    public static final String NAME = "get_report";

    private static final int DEFAULT_LIMIT = 40;
    private static final int MAX_LIMIT = 200;

    private static final List<String> CATEGORIES = List.of(
            "experiments", "projects", "variables", "germplasm", "scientific_objects", "facilities");

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Read the validation report of the uploaded file in full, for one category. The "
                + "system prompt only carries counts and a few examples, so call this when you need "
                + "the actual list: which variables are missing, which plots were not found, which "
                + "resources matched and at which URI. Categories: "
                + String.join(", ", CATEGORIES) + ".";
    }

    @Override
    public ObjectNode getParametersSchema(ObjectMapper mapper) {
        ObjectNode schema = ToolSchemas.object(mapper);
        ToolSchemas.string(schema, "category",
                "One of " + String.join(", ", CATEGORIES), true);
        ToolSchemas.string(schema, "status",
                "Optional filter: FOUND, AMBIGUOUS, FOUND_IN_SHARED_RESOURCE, MISSING or NOT_CHECKED",
                false);
        ToolSchemas.integer(schema, "limit", "Maximum entries, at most " + MAX_LIMIT, false);
        ToolSchemas.integer(schema, "offset", "Entries to skip, to page through a long category", false);
        return schema;
    }

    @Override
    public Object execute(JsonNode arguments, ToolContext context) {
        String category = ToolSchemas.text(arguments, "category");
        if (category == null) {
            return SearchVariablesTool.error("The 'category' argument is required.");
        }
        ResolutionReport report = context.getReport();
        if (report == null) {
            return SearchVariablesTool.error("The file has not been analysed yet.");
        }

        List<ResolvedItem> items = itemsOf(report, category);
        if (items == null) {
            Map<String, Object> error = SearchVariablesTool.error(
                    "There is no category named '" + category + "'.");
            error.put("available_categories", CATEGORIES);
            return error;
        }

        ResolutionStatus status = parseStatus(ToolSchemas.text(arguments, "status"));
        if (status != null) {
            List<ResolvedItem> filtered = new ArrayList<>();
            for (ResolvedItem item : items) {
                if (item.getStatus() == status) {
                    filtered.add(item);
                }
            }
            items = filtered;
        }

        int limit = ToolSchemas.number(arguments, "limit", DEFAULT_LIMIT, MAX_LIMIT);
        int offset = Math.max(0, ToolSchemas.number(arguments, "offset", 0, Integer.MAX_VALUE));
        offset = Math.min(offset, items.size());
        int end = Math.min(offset + limit, items.size());

        List<Map<String, Object>> entries = new ArrayList<>();
        for (ResolvedItem item : items.subList(offset, end)) {
            entries.add(describe(item));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("category", category);
        if (status != null) {
            response.put("status_filter", status.name());
        }
        response.put("total", items.size());
        response.put("offset", offset);
        response.put("returned", entries.size());
        response.put("entries", entries);
        if (end < items.size()) {
            response.put("note", (items.size() - end) + " more; call again with offset " + end + ".");
        }
        return response;
    }

    private Map<String, Object> describe(ResolvedItem item) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("value_in_file", item.getSourceValue());
        entry.put("status", item.getStatus().name());
        if (item.getExternalId() != null) {
            entry.put("ontology_id_in_file", item.getExternalId());
        }
        if (!item.getMatches().isEmpty()) {
            List<Map<String, Object>> matches = new ArrayList<>();
            for (ResourceReference match : item.getMatches()) {
                Map<String, Object> reference = new LinkedHashMap<>();
                reference.put("uri", String.valueOf(match.getUri()));
                reference.put("name", match.getName());
                if (match.getDatatype() != null) {
                    reference.put("datatype", match.getDatatype());
                }
                if (match.getSharedResourceInstanceLabel() != null) {
                    reference.put("shared_resource_instance", match.getSharedResourceInstanceLabel());
                }
                matches.add(reference);
            }
            entry.put("matches", matches);
        }
        if (item.getHint() != null) {
            entry.put("hint", item.getHint());
        }
        return entry;
    }

    private List<ResolvedItem> itemsOf(ResolutionReport report, String category) {
        switch (category.toLowerCase(Locale.ROOT)) {
            case "experiments":
                return report.getExperiments();
            case "projects":
                return report.getProjects();
            case "variables":
                return report.getVariables();
            case "germplasm":
                return report.getGermplasm();
            case "scientific_objects":
                return report.getScientificObjects();
            case "facilities":
                return report.getFacilities();
            default:
                return null;
        }
    }

    private ResolutionStatus parseStatus(String status) {
        if (status == null) {
            return null;
        }
        try {
            return ResolutionStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            // An unknown status filters nothing rather than failing: the model asked a reasonable
            // question with a wrong word, and the full category still answers it.
            return null;
        }
    }
}
