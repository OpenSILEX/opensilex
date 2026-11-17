//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.api.dto.ReportMessageDTO;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.graphimport.interpret.ClassCatalog;
import org.opensilex.graphimport.preview.NodeComparison;
import org.opensilex.graphimport.session.CreationRecord;
import org.opensilex.graphimport.write.CreationPlan;
import org.opensilex.graphimport.write.DataPlan;
import org.opensilex.graphimport.write.DataWriter;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * What the creation step would do, step by step, and what it already did.
 *
 * @author Arnaud Charleroy
 */
public class CreationPlanDTO {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("enabled")
    @ApiModelProperty(value = "Whether the platform lets the assistant write; off, the steps only say what they would do")
    private boolean enabled;

    @JsonProperty("steps")
    @ApiModelProperty(value = "The steps, in the order they run")
    private List<CreationStepDTO> steps;

    @JsonProperty("unsupported")
    @ApiModelProperty(value = "The resources to create that no step creates yet, by class")
    private List<ClassCountDTO> unsupported;

    @JsonProperty("history")
    @ApiModelProperty(value = "What the steps already wrote, oldest first")
    private List<CreationRecordDTO> history;

    /**
     * @param allowed the steps the user may run
     */
    public static CreationPlanDTO fromModel(String sessionId, boolean enabled, CreationPlan plan, DataPlan data,
                                            Set<String> allowed, List<CreationRecord> history, ClassCatalog catalog,
                                            String language) {
        CreationPlanDTO dto = new CreationPlanDTO();
        dto.sessionId = sessionId;
        dto.enabled = enabled;
        dto.steps = new ArrayList<>();
        plan.steps().forEach(step -> dto.steps.add(CreationStepDTO.fromModel(step, allowed.contains(step.name()))));
        dto.steps.add(CreationStepDTO.fromModel(data, allowed.contains(DataWriter.STEP)));
        dto.unsupported = new ArrayList<>();
        plan.unsupported().forEach((classUri, count) ->
                dto.unsupported.add(new ClassCountDTO(classUri, catalog.labelOf(classUri, language), count)));
        dto.history = history.stream().map(CreationRecordDTO::fromModel).toList();
        return dto;
    }

    public String getSessionId() {
        return sessionId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<CreationStepDTO> getSteps() {
        return steps;
    }

    public List<ClassCountDTO> getUnsupported() {
        return unsupported;
    }

    public List<CreationRecordDTO> getHistory() {
        return history;
    }

    /**
     * One step of the creation.
     */
    public static class CreationStepDTO {

        @JsonProperty("step")
        @ApiModelProperty(value = "The step: experiments, factors…")
        private String step;

        @JsonProperty("status")
        @ApiModelProperty(value = "READY, BLOCKED, DONE or EMPTY")
        private String status;

        @JsonProperty("after")
        @ApiModelProperty(value = "The steps whose resources must exist first")
        private List<String> after;

        @JsonProperty("allowed")
        @ApiModelProperty(value = "Whether the user may create these resources")
        private boolean allowed;

        @JsonProperty("ready")
        @ApiModelProperty(value = "The resources it would create now")
        private List<CreationNodeDTO> ready;

        @JsonProperty("blocked")
        @ApiModelProperty(value = "The resources it cannot create yet, with why")
        private List<CreationNodeDTO> blocked;

        @JsonProperty("done")
        @ApiModelProperty(value = "How many of its resources the instance already holds")
        private int done;

        @JsonProperty("count")
        @ApiModelProperty(value = "How many resources it would create now: what a check or a run of the step expects")
        private int count;

        @JsonProperty("blocked_count")
        @ApiModelProperty(value = "How many of its resources it cannot create yet")
        private int blockedCount;

        @JsonProperty("data")
        @ApiModelProperty(value = "For the step of data, which counts its data rather than listing them; absent otherwise")
        private DataStepDTO data;

        static CreationStepDTO fromModel(CreationPlan.Step step, boolean allowed) {
            CreationStepDTO dto = new CreationStepDTO();
            dto.step = step.name();
            dto.status = step.status().name();
            dto.after = step.writer().after();
            dto.allowed = allowed;
            dto.ready = step.ready().stream().map(node -> CreationNodeDTO.of(node, null)).toList();
            dto.blocked = step.blocked().stream().map(blocked -> CreationNodeDTO.of(blocked.node(), blocked.reason()))
                    .toList();
            dto.done = step.done();
            dto.count = dto.ready.size();
            dto.blockedCount = dto.blocked.size();
            return dto;
        }

        static CreationStepDTO fromModel(DataPlan plan, boolean allowed) {
            CreationStepDTO dto = new CreationStepDTO();
            dto.step = DataWriter.STEP;
            dto.status = plan.status().name();
            dto.after = DataWriter.after();
            dto.allowed = allowed;
            dto.ready = List.of();
            dto.blocked = plan.whyStopped().map(reason -> List.of(CreationNodeDTO.of(-1, null, reason)))
                    .orElse(List.of());
            dto.done = plan.done();
            dto.count = plan.ready().size();
            dto.blockedCount = plan.stopped() + plan.waiting();
            dto.data = DataStepDTO.fromModel(plan);
            return dto;
        }

        public String getStep() {
            return step;
        }

        public String getStatus() {
            return status;
        }

        public List<String> getAfter() {
            return after;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public List<CreationNodeDTO> getReady() {
            return ready;
        }

        public List<CreationNodeDTO> getBlocked() {
            return blocked;
        }

        public int getDone() {
            return done;
        }

        public int getCount() {
            return count;
        }

        public int getBlockedCount() {
            return blockedCount;
        }

        public DataStepDTO getData() {
            return data;
        }
    }

    /**
     * What the step of data counts: the data waiting for their targets, the cells that give none.
     */
    public static class DataStepDTO {

        @JsonProperty("waiting")
        @ApiModelProperty(value = "The data measured on rows of the files still to create")
        private int waiting;

        @JsonProperty("stopped")
        @ApiModelProperty(value = "The data that could be created but for what the whole step lacks")
        private int stopped;

        @JsonProperty("not_available")
        @ApiModelProperty(value = "The cells written as not available (NA): no datum")
        private int notAvailable;

        @JsonProperty("refused")
        @ApiModelProperty(value = "The cells that give no datum because of a problem, said at the step Understand")
        private int refused;

        @JsonProperty("sheets")
        @ApiModelProperty(value = "What each sheet of data gives")
        private List<DataSheetDTO> sheets;

        static DataStepDTO fromModel(DataPlan plan) {
            DataStepDTO dto = new DataStepDTO();
            dto.waiting = plan.waiting();
            dto.stopped = plan.stopped();
            dto.notAvailable = plan.notAvailable();
            dto.refused = plan.refused();
            dto.sheets = plan.sheets().stream().map(DataSheetDTO::fromModel).toList();
            return dto;
        }

        public int getWaiting() {
            return waiting;
        }

        public int getStopped() {
            return stopped;
        }

        public int getNotAvailable() {
            return notAvailable;
        }

        public int getRefused() {
            return refused;
        }

        public List<DataSheetDTO> getSheets() {
            return sheets;
        }
    }

    /**
     * A resource of a step, and why it cannot be created when it cannot.
     */
    public static class CreationNodeDTO {

        @JsonProperty("index")
        @ApiModelProperty(value = "Its index in the preview; -1 for what concerns the whole step")
        private int index;

        @JsonProperty("name")
        private String name;

        @JsonProperty("class_uri")
        private URI classUri;

        @JsonProperty("reason")
        @ApiModelProperty(value = "Why it cannot be created; absent when it can")
        private ReportMessageDTO reason;

        public static CreationNodeDTO of(NodeComparison node, ReportMessage reason) {
            CreationNodeDTO dto = new CreationNodeDTO();
            dto.index = node.index();
            dto.name = node.node().getName();
            dto.classUri = node.classUri();
            dto.reason = reason == null ? null : ReportMessageDTO.fromModel(reason);
            return dto;
        }

        public static CreationNodeDTO of(int index, String name, ReportMessage reason) {
            CreationNodeDTO dto = new CreationNodeDTO();
            dto.index = index;
            dto.name = name;
            dto.reason = reason == null ? null : ReportMessageDTO.fromModel(reason);
            return dto;
        }

        public int getIndex() {
            return index;
        }

        public String getName() {
            return name;
        }

        public URI getClassUri() {
            return classUri;
        }

        public ReportMessageDTO getReason() {
            return reason;
        }
    }

    /**
     * How many resources of a class are to create.
     */
    public static class ClassCountDTO {

        @JsonProperty("class_uri")
        private URI classUri;

        @JsonProperty("label")
        private String label;

        @JsonProperty("count")
        private int count;

        ClassCountDTO(URI classUri, String label, int count) {
            this.classUri = classUri;
            this.label = label;
            this.count = count;
        }

        public URI getClassUri() {
            return classUri;
        }

        public String getLabel() {
            return label;
        }

        public int getCount() {
            return count;
        }
    }

    /**
     * A step that wrote to the instance.
     */
    public static class CreationRecordDTO {

        @JsonProperty("step")
        private String step;

        @JsonProperty("at")
        private Instant at;

        @JsonProperty("created")
        private int created;

        static CreationRecordDTO fromModel(CreationRecord record) {
            CreationRecordDTO dto = new CreationRecordDTO();
            dto.step = record.step();
            dto.at = record.at();
            dto.created = record.created();
            return dto;
        }

        public String getStep() {
            return step;
        }

        public Instant getAt() {
            return at;
        }

        public int getCreated() {
            return created;
        }
    }
}
