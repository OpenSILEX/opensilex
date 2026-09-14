//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.mapping.ColumnMapping;
import org.opensilex.aiimport.mapping.TypeIssue;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * One column of the file: what it stands for, what it matched, what its cells hold, and where the
 * two disagree.
 *
 * @author Arnaud Charleroy
 */
public class ColumnMappingDTO {

    private List<String> sheets = new ArrayList<>();
    private String column;

    private String role;

    @JsonProperty("target_entity")
    private String targetEntity;

    @JsonProperty("role_explanation")
    private String roleExplanation;

    @JsonProperty("resolved_uri")
    private URI resolvedUri;

    @JsonProperty("resolved_name")
    private String resolvedName;

    @JsonProperty("resolution_status")
    private String resolutionStatus;

    @JsonProperty("expected_datatype")
    private String expectedDatatype;

    @JsonProperty("observed_kind")
    private String observedKind;

    @JsonProperty("value_count")
    private int valueCount;

    @JsonProperty("missing_count")
    private int missingCount;

    @JsonProperty("sample_values")
    private List<String> sampleValues = new ArrayList<>();

    private List<TypeIssueDTO> issues = new ArrayList<>();

    private String suggestion;

    @JsonProperty("suggestion_message")
    private ReportMessageDTO suggestionMessage;

    /**
     * A cell that will not go in as it stands.
     */
    public static class TypeIssueDTO {

        private String sheet;

        @JsonProperty("row_number")
        private int rowNumber;

        private String value;
        private String problem;
        private String suggestion;

        @JsonProperty("problem_message")
        private ReportMessageDTO problemMessage;

        @JsonProperty("suggestion_message")
        private ReportMessageDTO suggestionMessage;

        public static TypeIssueDTO fromModel(TypeIssue model) {
            TypeIssueDTO dto = new TypeIssueDTO();
            dto.sheet = model.getSheet();
            dto.rowNumber = model.getRowNumber();
            dto.value = model.getValue();
            dto.problem = model.getProblem();
            dto.suggestion = model.getSuggestion();
            dto.problemMessage = ReportMessageDTO.fromModel(model.getProblemMessage());
            dto.suggestionMessage = ReportMessageDTO.fromModel(model.getSuggestionMessage());
            return dto;
        }

        @ApiModelProperty(example = "10_Controle_Maturite")
        public String getSheet() {
            return sheet;
        }

        public void setSheet(String sheet) {
            this.sheet = sheet;
        }

        @ApiModelProperty(value = "Row number as it appears in the spreadsheet")
        public int getRowNumber() {
            return rowNumber;
        }

        public void setRowNumber(int rowNumber) {
            this.rowNumber = rowNumber;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getProblem() {
            return problem;
        }

        public void setProblem(String problem) {
            this.problem = problem;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }

        public ReportMessageDTO getProblemMessage() {
            return problemMessage;
        }

        public void setProblemMessage(ReportMessageDTO problemMessage) {
            this.problemMessage = problemMessage;
        }

        public ReportMessageDTO getSuggestionMessage() {
            return suggestionMessage;
        }

        public void setSuggestionMessage(ReportMessageDTO suggestionMessage) {
            this.suggestionMessage = suggestionMessage;
        }
    }

    public static ColumnMappingDTO fromModel(ColumnMapping model) {
        ColumnMappingDTO dto = new ColumnMappingDTO();
        dto.sheets = model.getSheets();
        dto.column = model.getColumn();
        dto.role = model.getRole().name();
        dto.targetEntity = model.getRole().getEntity();
        dto.roleExplanation = model.getRole().getExplanation();
        dto.resolvedUri = model.getResolvedUri();
        dto.resolvedName = model.getResolvedName();
        dto.resolutionStatus = model.getResolutionStatus() == null
                ? null
                : model.getResolutionStatus().name();
        dto.expectedDatatype = model.getExpectedDatatype();
        dto.observedKind = model.getObservedKind().name();
        dto.valueCount = model.getValueCount();
        dto.missingCount = model.getMissingCount();
        dto.sampleValues = model.getSampleValues();
        dto.suggestion = model.getSuggestion();
        dto.suggestionMessage = ReportMessageDTO.fromModel(model.getSuggestionMessage());
        for (TypeIssue issue : model.getIssues()) {
            dto.issues.add(TypeIssueDTO.fromModel(issue));
        }
        return dto;
    }

    @ApiModelProperty(value = "The sheets this column appears in")
    public List<String> getSheets() {
        return sheets;
    }

    public void setSheets(List<String> sheets) {
        this.sheets = sheets;
    }

    @ApiModelProperty(example = "Bai_Suc_g")
    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    @ApiModelProperty(value = "What the column stands for", example = "VARIABLE")
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @ApiModelProperty(value = "The OpenSILEX concept this column feeds", example = "Variable")
    public String getTargetEntity() {
        return targetEntity;
    }

    public void setTargetEntity(String targetEntity) {
        this.targetEntity = targetEntity;
    }

    public String getRoleExplanation() {
        return roleExplanation;
    }

    public void setRoleExplanation(String roleExplanation) {
        this.roleExplanation = roleExplanation;
    }

    public URI getResolvedUri() {
        return resolvedUri;
    }

    public void setResolvedUri(URI resolvedUri) {
        this.resolvedUri = resolvedUri;
    }

    public String getResolvedName() {
        return resolvedName;
    }

    public void setResolvedName(String resolvedName) {
        this.resolvedName = resolvedName;
    }

    public String getResolutionStatus() {
        return resolutionStatus;
    }

    public void setResolutionStatus(String resolutionStatus) {
        this.resolutionStatus = resolutionStatus;
    }

    @ApiModelProperty(value = "The data type the matched variable expects")
    public String getExpectedDatatype() {
        return expectedDatatype;
    }

    public void setExpectedDatatype(String expectedDatatype) {
        this.expectedDatatype = expectedDatatype;
    }

    @ApiModelProperty(value = "What the cells actually hold", example = "DECIMAL")
    public String getObservedKind() {
        return observedKind;
    }

    public void setObservedKind(String observedKind) {
        this.observedKind = observedKind;
    }

    public int getValueCount() {
        return valueCount;
    }

    public void setValueCount(int valueCount) {
        this.valueCount = valueCount;
    }

    public int getMissingCount() {
        return missingCount;
    }

    public void setMissingCount(int missingCount) {
        this.missingCount = missingCount;
    }

    public List<String> getSampleValues() {
        return sampleValues;
    }

    public void setSampleValues(List<String> sampleValues) {
        this.sampleValues = sampleValues;
    }

    public List<TypeIssueDTO> getIssues() {
        return issues;
    }

    public void setIssues(List<TypeIssueDTO> issues) {
        this.issues = issues;
    }

    @ApiModelProperty(value = "What to do about this column, when something needs doing")
    public String getSuggestion() {
        return suggestion;
    }

    public ReportMessageDTO getSuggestionMessage() {
        return suggestionMessage;
    }

    public void setSuggestionMessage(ReportMessageDTO suggestionMessage) {
        this.suggestionMessage = suggestionMessage;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }
}
