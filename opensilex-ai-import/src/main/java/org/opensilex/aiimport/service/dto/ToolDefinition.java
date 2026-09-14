//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Declaration of a tool the model may call, in the OpenAI-compatible {@code function} shape.
 *
 * @author Arnaud Charleroy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolDefinition {

    private String type = "function";
    private Function function;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Function {

        private String name;
        private String description;

        /**
         * A JSON Schema object describing the accepted arguments.
         */
        private ObjectNode parameters;

        public String getName() {
            return name;
        }

        public Function setName(String name) {
            this.name = name;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public Function setDescription(String description) {
            this.description = description;
            return this;
        }

        public ObjectNode getParameters() {
            return parameters;
        }

        public Function setParameters(ObjectNode parameters) {
            this.parameters = parameters;
            return this;
        }
    }

    public static ToolDefinition of(String name, String description, ObjectNode parameters) {
        ToolDefinition definition = new ToolDefinition();
        definition.function = new Function()
                .setName(name)
                .setDescription(description)
                .setParameters(parameters);
        return definition;
    }

    public String getType() {
        return type;
    }

    public ToolDefinition setType(String type) {
        this.type = type;
        return this;
    }

    public Function getFunction() {
        return function;
    }

    public ToolDefinition setFunction(Function function) {
        this.function = function;
        return this;
    }
}
