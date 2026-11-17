//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.graphimport.binding.ImportDecisions;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * What the user decided about a sheet: its class, the column identifying its rows, the property of
 * some columns, the columns left out, the values given for every row. It replaces what was decided before about that sheet; a column it
 * does not mention goes back to its suggestion.
 *
 * @author Arnaud Charleroy
 */
public class SheetDecisionsDTO {

    @NotNull
    @JsonProperty("session_id")
    @ApiModelProperty(value = "The import", required = true)
    private String sessionId;

    @NotNull
    @JsonProperty("sheet")
    @ApiModelProperty(value = "The sheet, as the import names it", required = true)
    private String sheet;

    @JsonProperty("class_uri")
    @ApiModelProperty(value = "The class of the rows; required to decide the property of a column")
    private URI classUri;

    @Min(0)
    @JsonProperty("identifier_column")
    @ApiModelProperty(value = "The column identifying the rows, from 0", example = "0")
    private Integer identifierColumn;

    @Valid
    @JsonProperty("columns")
    @ApiModelProperty(value = "The property of each decided column")
    private List<ColumnDecisionDTO> columns = new ArrayList<>();

    @JsonProperty("left_out")
    @ApiModelProperty(value = "The columns left out, from 0: they give no value and are not suggested again")
    private List<Integer> leftOut = new ArrayList<>();

    @Valid
    @JsonProperty("given")
    @ApiModelProperty(value = "Values given for every row, for properties no column gives")
    private List<PropertyValueDTO> given = new ArrayList<>();

    public ImportDecisions.SheetDecisions toModel() {
        Map<Integer, URI> properties = new HashMap<>();
        Map<Integer, URI> measures = new HashMap<>();
        if (columns != null) {
            columns.forEach(column -> {
                properties.put(column.getColumn(), column.getProperty());
                if (column.getVariable() != null) {
                    measures.put(column.getColumn(), column.getVariable());
                }
            });
        }
        Set<Integer> left = new HashSet<>();
        if (leftOut != null) {
            leftOut.stream().filter(Objects::nonNull).forEach(left::add);
        }
        Map<URI, String> values = new HashMap<>();
        if (given != null) {
            given.stream().filter(value -> value != null && value.getProperty() != null)
                    .forEach(value -> values.put(value.getProperty(), value.getValue()));
        }
        return new ImportDecisions.SheetDecisions(classUri, identifierColumn, properties, Map.of(), left, values,
                measures);
    }

    public List<PropertyValueDTO> getGiven() {
        return given;
    }

    public void setGiven(List<PropertyValueDTO> given) {
        this.given = given;
    }

    public List<Integer> getLeftOut() {
        return leftOut;
    }

    public void setLeftOut(List<Integer> leftOut) {
        this.leftOut = leftOut;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSheet() {
        return sheet;
    }

    public void setSheet(String sheet) {
        this.sheet = sheet;
    }

    public URI getClassUri() {
        return classUri;
    }

    public void setClassUri(URI classUri) {
        this.classUri = classUri;
    }

    public Integer getIdentifierColumn() {
        return identifierColumn;
    }

    public void setIdentifierColumn(Integer identifierColumn) {
        this.identifierColumn = identifierColumn;
    }

    public List<ColumnDecisionDTO> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnDecisionDTO> columns) {
        this.columns = columns;
    }

    /**
     * The property the user chose for a column.
     */
    public static class ColumnDecisionDTO {

        @NotNull
        @Min(0)
        @JsonProperty("column")
        @ApiModelProperty(value = "The column, from 0", example = "2", required = true)
        private Integer column;

        @NotNull
        @JsonProperty("property")
        @ApiModelProperty(value = "The property it holds", required = true)
        private URI property;

        @JsonProperty("variable")
        @ApiModelProperty(value = "For a column of measures in a sheet of data, the variable it measures")
        private URI variable;

        public URI getVariable() {
            return variable;
        }

        public void setVariable(URI variable) {
            this.variable = variable;
        }

        public Integer getColumn() {
            return column;
        }

        public void setColumn(Integer column) {
            this.column = column;
        }

        public URI getProperty() {
            return property;
        }

        public void setProperty(URI property) {
            this.property = property;
        }
    }
}
