//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A tool invocation requested by the model.
 *
 * @author Arnaud Charleroy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ToolCall {

    private String id;
    private String type = "function";
    private FunctionCall function;

    /**
     * Name and arguments of the requested call. Arguments arrive as a JSON string, not as an
     * object, which is what the OpenAI-compatible contract specifies.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FunctionCall {

        private String name;
        private String arguments;

        public String getName() {
            return name;
        }

        public FunctionCall setName(String name) {
            this.name = name;
            return this;
        }

        public String getArguments() {
            return arguments;
        }

        public FunctionCall setArguments(String arguments) {
            this.arguments = arguments;
            return this;
        }
    }

    public String getId() {
        return id;
    }

    public ToolCall setId(String id) {
        this.id = id;
        return this;
    }

    public String getType() {
        return type;
    }

    public ToolCall setType(String type) {
        this.type = type;
        return this;
    }

    public FunctionCall getFunction() {
        return function;
    }

    public ToolCall setFunction(FunctionCall function) {
        this.function = function;
        return this;
    }

    public String getFunctionName() {
        return function == null ? null : function.getName();
    }

    public String getFunctionArguments() {
        return function == null ? null : function.getArguments();
    }
}
