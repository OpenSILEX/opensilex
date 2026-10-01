//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensilex.aiimport.create.CreationRequirements;
import org.opensilex.aiimport.create.CreationTarget;
import org.opensilex.aiimport.create.RequiredField;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Says what a creation would take, before drafting one.
 * <p>
 * The prompt carries a summary of this, so the tool exists for the cases the summary cannot cover —
 * after a creation has changed what the next one needs, or when the assistant wants the exact field
 * names rather than working from memory.
 *
 * @author Arnaud Charleroy
 */
public class GetCreationFieldsTool implements AiTool {

    public static final String NAME = "get_creation_fields";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "List the fields a PROJECT, an EXPERIMENT, its FACTORS, its SCIENTIFIC_OBJECTS, the trial EVENT records or "
                + "the observation DATA need, which of "
                + "them are required, what the uploaded file already suggests for each, and what "
                + "currently prevents the creation. Call it before propose_creation when you are "
                + "unsure of a field name, or after something was created, since that changes what "
                + "the next creation requires.";
    }

    @Override
    public ObjectNode getParametersSchema(ObjectMapper mapper) {
        ObjectNode schema = ToolSchemas.object(mapper);
        ToolSchemas.string(schema, "target", CreationTarget.choices(), true);
        return schema;
    }

    @Override
    public Object execute(JsonNode arguments, ToolContext context) {
        String rawTarget = ToolSchemas.text(arguments, "target");
        if (rawTarget == null) {
            return ToolSchemas.error("The 'target' argument is required.");
        }
        CreationTarget target = CreationTarget.parse(rawTarget).orElse(null);
        if (target == null) {
            return ToolSchemas.error(
                    "'" + rawTarget + "' is not one of " + CreationTarget.choices() + ".");
        }
        if (context.getSession() == null) {
            return ToolSchemas.error("There is no open conversation.");
        }

        CreationRequirements requirements =
                context.getCreationService().requirementsFor(target, context.getSession());

        List<Map<String, Object>> fields = new ArrayList<>();
        for (RequiredField field : requirements.getFields()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", field.getName());
            entry.put("kind", field.getKind());
            entry.put("required", field.isRequired());
            if (field.getSuggestedValue() != null) {
                entry.put("the_file_suggests", field.getSuggestedValue());
            }
            fields.add(entry);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("target", target.name());
        response.put("available_now", requirements.isAvailable());
        response.put("fields", fields);
        if (!requirements.getBlockers().isEmpty()) {
            response.put("blocked_because", requirements.getBlockers());
        }
        if (!requirements.getWarnings().isEmpty()) {
            response.put("warnings", requirements.getWarnings());
        }
        return response;
    }
}
