//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensilex.aiimport.create.CreationProposal;
import org.opensilex.aiimport.create.CreationTarget;
import org.opensilex.aiimport.create.ProposalBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Drafts a creation for the user to confirm.
 * <p>
 * This is the only way the assistant reaches a write, and it does not write: it produces a draft
 * that appears in the conversation with its own confirm button. What it returns is deliberately
 * useful to the model — the normalised values and the fields still missing — so it can say what it
 * needs in the same turn instead of proposing something incomplete and waiting to be told.
 *
 * @author Arnaud Charleroy
 */
public class ProposeCreationTool implements AiTool {

    public static final String NAME = "propose_creation";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Draft the creation of a PROJECT, an EXPERIMENT, its FACTORS (the treatments of the "
                + "file as factor levels), its SCIENTIFIC_OBJECTS (every observed unit of the file in one "
                + "pass), the import of the VARIABLE definitions from a shared "
                + "resource instance, the trial EVENT records, or the insertion of the observation "
                + "DATA, for the user to confirm. Nothing is written: the draft appears in the "
                + "conversation with a confirm button, and the user may correct any value first. "
                + "Describe what you are about to propose in your reply before calling this. Pass "
                + "only the fields you are sure of; anything the file already answers is filled in "
                + "for you. If a required field is still missing, the result says which, and you "
                + "must ask the user rather than inventing it.";
    }

    @Override
    public ObjectNode getParametersSchema(ObjectMapper mapper) {
        ObjectNode schema = ToolSchemas.object(mapper);
        ToolSchemas.string(schema, "target", CreationTarget.choices(), true);

        ObjectNode fields = ((ObjectNode) schema.get("properties")).putObject("fields");
        fields.put("type", "object");
        fields.put("description", "The field values, keyed by field name. Call "
                + "get_creation_fields if you are unsure which names exist.");
        fields.putObject("additionalProperties").put("type", "string");

        ToolSchemas.string(schema, "rationale",
                "One sentence on why these values, shown above the draft", false);
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
            return ToolSchemas.error("There is no open conversation to propose against.");
        }

        Map<String, String> fields = readFields(arguments);
        CreationProposal proposal;
        try {
            proposal = new ProposalBuilder(context.getCreationService())
                    .build(context.getSession(), target, fields,
                            ToolSchemas.text(arguments, "rationale"));
        } catch (IllegalArgumentException e) {
            // The assistant's own mistake — a field it invented, a date it malformed. Telling it
            // lets it correct itself; raising would lose the turn.
            return ToolSchemas.error(e.getMessage());
        }

        context.getSession().setPendingProposal(proposal);
        return describe(proposal);
    }

    private Map<String, String> readFields(JsonNode arguments) {
        Map<String, String> fields = new LinkedHashMap<>();
        JsonNode node = arguments == null ? null : arguments.get("fields");
        if (node == null || !node.isObject()) {
            return fields;
        }
        node.fieldNames().forEachRemaining(name -> {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull()) {
                fields.put(name, value.asText());
            }
        });
        return fields;
    }

    private Map<String, Object> describe(CreationProposal proposal) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("proposal_id", proposal.getId());
        response.put("target", proposal.getTarget().name());
        response.put("fields", proposal.getFields());

        Map<String, String> sources = new LinkedHashMap<>();
        proposal.getFieldSources().forEach((field, source) -> sources.put(field, source.name()));
        response.put("value_came_from", sources);

        if (!proposal.getMissingRequired().isEmpty()) {
            response.put("still_missing", proposal.getMissingRequired());
            response.put("what_to_do", "Ask the user for "
                    + String.join(", ", proposal.getMissingRequired())
                    + ". Do not invent a value. Call propose_creation again with their answer.");
        }
        if (!proposal.getBlockers().isEmpty()) {
            response.put("blocked_because", proposal.getBlockers());
            response.put("what_to_do",
                    "This cannot be created yet. Explain why, and what has to exist first.");
        }
        if (proposal.isReady()) {
            List<String> next = new ArrayList<>();
            next.add("The draft is shown to the user with a confirm button.");
            next.add("Tell them what it will create; do not say you created it.");
            response.put("what_to_do", String.join(" ", next));
        }
        return response;
    }
}
