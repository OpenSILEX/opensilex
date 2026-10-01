//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small helpers for writing the JSON Schema of a tool without a wall of node building.
 *
 * @author Arnaud Charleroy
 */
public class ToolSchemas {

    private ToolSchemas() {
    }

    public static ObjectNode object(ObjectMapper mapper) {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties");
        schema.putArray("required");
        return schema;
    }

    public static ObjectNode string(ObjectNode schema, String name, String description, boolean required) {
        ObjectNode property = ((ObjectNode) schema.get("properties")).putObject(name);
        property.put("type", "string");
        property.put("description", description);
        if (required) {
            ((ArrayNode) schema.get("required")).add(name);
        }
        return schema;
    }

    public static ObjectNode integer(ObjectNode schema, String name, String description, boolean required) {
        ObjectNode property = ((ObjectNode) schema.get("properties")).putObject(name);
        property.put("type", "integer");
        property.put("description", description);
        if (required) {
            ((ArrayNode) schema.get("required")).add(name);
        }
        return schema;
    }

    /**
     * @return the string at {@code field}, or {@code null} when absent, null or blank
     */
    public static String text(com.fasterxml.jackson.databind.JsonNode arguments, String field) {
        if (arguments == null || !arguments.hasNonNull(field)) {
            return null;
        }
        String value = arguments.get(field).asText().trim();
        return value.isEmpty() ? null : value;
    }

    public static int number(com.fasterxml.jackson.databind.JsonNode arguments, String field,
                             int defaultValue, int max) {
        if (arguments == null || !arguments.hasNonNull(field)) {
            return defaultValue;
        }
        int value = arguments.get(field).asInt(defaultValue);
        if (value <= 0) {
            return defaultValue;
        }
        return Math.min(value, max);
    }

    /**
     * What a tool answers when it cannot: a single {@code error} field, which the model reads and
     * relays instead of an exception it would never see.
     */
    public static Map<String, Object> error(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error", message);
        return response;
    }
}
