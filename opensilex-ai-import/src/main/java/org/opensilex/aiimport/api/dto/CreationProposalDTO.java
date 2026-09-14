//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.create.CreationProposal;
import org.opensilex.aiimport.create.CreationRequirements;
import org.opensilex.aiimport.create.RequiredField;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A creation the assistant drafted, as the card in the conversation shows it.
 *
 * @author Arnaud Charleroy
 */
public class CreationProposalDTO {

    private String id;
    private String target;
    private String status;
    private String rationale;

    private List<FieldDTO> fields = new ArrayList<>();

    @JsonProperty("missing_required")
    private List<String> missingRequired = new ArrayList<>();

    private List<String> blockers = new ArrayList<>();

    @JsonProperty("is_ready")
    private boolean ready;

    @JsonProperty("result_uri")
    private URI resultUri;

    @JsonProperty("inserted_count")
    private Integer insertedCount;

    /**
     * One field of the draft, with what it holds and where that came from.
     */
    public static class FieldDTO {

        private String name;

        @JsonProperty("label_key")
        private String labelKey;

        private String kind;

        private String resource;
        private boolean required;
        private String value;
        private String source;

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

        @ApiModelProperty(value = "text, date, uri or uri-list")
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

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @ApiModelProperty(value = "FILE, ASSISTANT or USER — where the value came from")
        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }

    /**
     * @param requirements the current requirements, which carry the label and kind of each field.
     *                     Passing them keeps the card's labels in step with the server's rules
     *                     instead of duplicating them on the proposal.
     */
    public static CreationProposalDTO fromModel(CreationProposal model,
                                                CreationRequirements requirements) {
        CreationProposalDTO dto = new CreationProposalDTO();
        dto.id = model.getId();
        dto.target = model.getTarget().name();
        dto.status = model.getStatus().name();
        dto.rationale = model.getRationale();
        dto.missingRequired = model.getMissingRequired();
        dto.blockers = model.getBlockers();
        dto.ready = model.isReady();
        dto.resultUri = model.getResultUri();
        dto.insertedCount = model.getInsertedCount();

        Map<String, RequiredField> known = new LinkedHashMap<>();
        if (requirements != null) {
            requirements.getFields().forEach(field -> known.put(field.getName(), field));
        }

        // Ordered by the requirements, so the card reads the same way every time; anything the
        // draft holds that the requirements no longer declare is still shown rather than dropped.
        List<String> order = new ArrayList<>(known.keySet());
        model.getFields().keySet().forEach(name -> {
            if (!order.contains(name)) {
                order.add(name);
            }
        });

        for (String name : order) {
            if (!model.getFields().containsKey(name) && !isRequired(known.get(name))) {
                continue;
            }
            RequiredField field = known.get(name);
            FieldDTO entry = new FieldDTO();
            entry.name = name;
            entry.labelKey = field == null ? null : field.getLabelKey();
            entry.kind = field == null ? RequiredField.KIND_TEXT : field.getKind();
            entry.resource = field == null ? null : field.getResource();
            entry.required = isRequired(field);
            entry.value = model.getFields().get(name);
            entry.source = model.getFieldSources().containsKey(name)
                    ? model.getFieldSources().get(name).name()
                    : null;
            dto.fields.add(entry);
        }
        return dto;
    }

    private static boolean isRequired(RequiredField field) {
        return field != null && field.isRequired();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(example = "EXPERIMENT")
    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    @ApiModelProperty(value = "PENDING, APPLIED or CANCELLED")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @ApiModelProperty(value = "Why the assistant proposed these values")
    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public List<FieldDTO> getFields() {
        return fields;
    }

    public void setFields(List<FieldDTO> fields) {
        this.fields = fields;
    }

    public List<String> getMissingRequired() {
        return missingRequired;
    }

    public void setMissingRequired(List<String> missingRequired) {
        this.missingRequired = missingRequired;
    }

    public List<String> getBlockers() {
        return blockers;
    }

    public void setBlockers(List<String> blockers) {
        this.blockers = blockers;
    }

    @ApiModelProperty(value = "False while a required field is empty or something blocks it")
    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public URI getResultUri() {
        return resultUri;
    }

    public void setResultUri(URI resultUri) {
        this.resultUri = resultUri;
    }

    public Integer getInsertedCount() {
        return insertedCount;
    }

    public void setInsertedCount(Integer insertedCount) {
        this.insertedCount = insertedCount;
    }
}
