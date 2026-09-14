//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.create.CreationRequirements;
import org.opensilex.aiimport.create.RequiredField;

import java.util.ArrayList;
import java.util.List;

/**
 * What it would take to create one kind of resource from the open conversation.
 *
 * @author Arnaud Charleroy
 */
public class CreationRequirementsDTO {

    private String target;

    private List<RequiredFieldDTO> fields = new ArrayList<>();

    private List<String> blockers = new ArrayList<>();

    private List<String> warnings = new ArrayList<>();

    @JsonProperty("is_available")
    private boolean available;

    /**
     * One field of the creation form.
     */
    public static class RequiredFieldDTO {

        private String name;

        @JsonProperty("label_key")
        private String labelKey;

        private String kind;

        /**
         * The referential this field designates, so the interface can use the selector that
         * already exists for it.
         */
        private String resource;

        private boolean required;

        @JsonProperty("suggested_value")
        private String suggestedValue;

        @JsonProperty("suggested_from")
        private String suggestedFrom;

        public static RequiredFieldDTO fromModel(RequiredField model) {
            RequiredFieldDTO dto = new RequiredFieldDTO();
            dto.name = model.getName();
            dto.labelKey = model.getLabelKey();
            dto.kind = model.getKind();
            dto.resource = model.getResource();
            dto.required = model.isRequired();
            dto.suggestedValue = model.getSuggestedValue();
            dto.suggestedFrom = model.getSuggestedFrom();
            return dto;
        }

        @ApiModelProperty(example = "start_date")
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLabelKey() {
            return labelKey;
        }

        public void setLabelKey(String labelKey) {
            this.labelKey = labelKey;
        }

        @ApiModelProperty(value = "text, date, uri or uri-list", example = "date")
        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        @ApiModelProperty(value = "What the file suggests, as a default the user may change")
        public String getSuggestedValue() {
            return suggestedValue;
        }

        public void setSuggestedValue(String suggestedValue) {
            this.suggestedValue = suggestedValue;
        }

        @ApiModelProperty(value = "Translation key saying where the suggestion came from")
        public String getSuggestedFrom() {
            return suggestedFrom;
        }

        public void setSuggestedFrom(String suggestedFrom) {
            this.suggestedFrom = suggestedFrom;
        }
    }

    public static CreationRequirementsDTO fromModel(CreationRequirements model) {
        CreationRequirementsDTO dto = new CreationRequirementsDTO();
        dto.target = model.getTarget().name();
        dto.blockers = model.getBlockers();
        dto.warnings = model.getWarnings();
        dto.available = model.isAvailable();
        for (RequiredField field : model.getFields()) {
            dto.fields.add(RequiredFieldDTO.fromModel(field));
        }
        return dto;
    }

    @ApiModelProperty(value = "PROJECT, EXPERIMENT or DATA", example = "EXPERIMENT")
    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public List<RequiredFieldDTO> getFields() {
        return fields;
    }

    public void setFields(List<RequiredFieldDTO> fields) {
        this.fields = fields;
    }

    @ApiModelProperty(value = "Translation keys for what prevents the creation outright")
    public List<String> getBlockers() {
        return blockers;
    }

    public void setBlockers(List<String> blockers) {
        this.blockers = blockers;
    }

    @ApiModelProperty(value = "Translation keys for what is worth knowing but does not prevent it")
    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    @ApiModelProperty(value = "False when a blocker stands in the way")
    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
