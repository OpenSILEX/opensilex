//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensilex.aiimport.mapping.ColumnMapping;
import org.opensilex.aiimport.mapping.TypeIssue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Returns the full mapping of one column, offending cells included.
 * <p>
 * The prompt lists the columns needing attention with a count of their bad cells. Quoting every
 * cell of every column up front costs a great deal and is read for at most one of them.
 *
 * @author Arnaud Charleroy
 */
public class GetColumnMappingTool implements AiTool {

    public static final String NAME = "get_mapping";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Read how one column of the uploaded file was mapped: what it stands for, which "
                + "variable it matched, the data type that variable expects, what its cells actually "
                + "hold, and every cell that disagrees with its row number in the spreadsheet. Call "
                + "it before advising on a column, and omit the column name to list them all briefly.";
    }

    @Override
    public ObjectNode getParametersSchema(ObjectMapper mapper) {
        ObjectNode schema = ToolSchemas.object(mapper);
        ToolSchemas.string(schema, "column",
                "Exact column header. Omit to get a one-line summary of every column.", false);
        return schema;
    }

    @Override
    public Object execute(JsonNode arguments, ToolContext context) {
        List<ColumnMapping> mappings = context.getMappings();
        if (mappings.isEmpty()) {
            return SearchVariablesTool.error("The file has not been analysed yet.");
        }

        String column = ToolSchemas.text(arguments, "column");
        if (column == null) {
            return listAll(mappings);
        }

        List<Map<String, Object>> matches = new ArrayList<>();
        for (ColumnMapping mapping : mappings) {
            if (mapping.getColumn().equalsIgnoreCase(column)) {
                matches.add(describe(mapping));
            }
        }
        if (matches.isEmpty()) {
            Map<String, Object> error = SearchVariablesTool.error(
                    "No column named '" + column + "' in the uploaded file.");
            List<String> names = new ArrayList<>();
            mappings.forEach(mapping -> names.add(mapping.getColumn()));
            error.put("available_columns", names);
            return error;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("column", column);
        response.put("mappings", matches);
        return response;
    }

    private Map<String, Object> listAll(List<ColumnMapping> mappings) {
        List<Map<String, Object>> lines = new ArrayList<>();
        for (ColumnMapping mapping : mappings) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("column", mapping.getColumn());
            line.put("stands_for", mapping.getRole().name());
            line.put("values_look_like", mapping.getObservedKind().name());
            line.put("needs_attention", mapping.needsAttention());
            lines.add(line);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", lines.size());
        response.put("columns", lines);
        return response;
    }

    private Map<String, Object> describe(ColumnMapping mapping) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("column", mapping.getColumn());
        entry.put("sheets", mapping.getSheets());
        entry.put("stands_for", mapping.getRole().name());
        entry.put("feeds", mapping.getRole().getEntity());
        entry.put("role_explanation", mapping.getRole().getExplanation());

        if (mapping.getResolvedName() != null) {
            entry.put("matched_variable", mapping.getResolvedName());
            entry.put("matched_uri", String.valueOf(mapping.getResolvedUri()));
        }
        if (mapping.getResolutionStatus() != null) {
            entry.put("resolution_status", mapping.getResolutionStatus().name());
        }
        if (mapping.getExpectedDatatype() != null) {
            entry.put("expected_type", mapping.getExpectedDatatype());
        }
        entry.put("values_look_like", mapping.getObservedKind().name());
        entry.put("value_count", mapping.getValueCount());
        entry.put("missing_count", mapping.getMissingCount());
        entry.put("examples", mapping.getSampleValues());

        if (mapping.getSuggestion() != null) {
            entry.put("suggestion", mapping.getSuggestion());
        }
        if (mapping.hasIssues()) {
            List<Map<String, Object>> issues = new ArrayList<>();
            for (TypeIssue issue : mapping.getIssues()) {
                Map<String, Object> line = new LinkedHashMap<>();
                line.put("sheet", issue.getSheet());
                line.put("row", issue.getRowNumber());
                line.put("value", issue.getValue());
                line.put("problem", issue.getProblem());
                if (issue.getSuggestion() != null) {
                    line.put("suggestion", issue.getSuggestion());
                }
                issues.add(line);
            }
            entry.put("offending_cells", issues);
        }
        return entry;
    }
}
