//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.create.steps.StepState;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Where one step of the import stands: what the guided path of the screen draws a line from.
 *
 * @author Arnaud Charleroy
 */
public class ImportStepDTO {

    private String step;

    @JsonProperty("label_key")
    private String labelKey;

    private String status;

    @JsonProperty("blocked_by")
    private List<String> blockedBy = new ArrayList<>();

    @JsonProperty("depends_on")
    private List<String> dependsOn = new ArrayList<>();

    private int done;
    private int total;
    private String target;

    public static ImportStepDTO fromModel(StepState model) {
        ImportStepDTO dto = new ImportStepDTO();
        dto.step = model.step().name();
        dto.labelKey = model.step().labelKey();
        dto.status = model.status().name();
        dto.blockedBy = model.blockedBy().stream().map(Enum::name).collect(Collectors.toList());
        dto.dependsOn = model.step().dependsOn().stream().map(Enum::name).collect(Collectors.toList());
        dto.done = model.done();
        dto.total = model.total();
        dto.target = model.step().target().map(Enum::name).orElse(null);
        return dto;
    }

    @ApiModelProperty(value = "The step", example = "FACTORS")
    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    @ApiModelProperty(value = "Translation key of the step's name")
    public String getLabelKey() {
        return labelKey;
    }

    public void setLabelKey(String labelKey) {
        this.labelKey = labelKey;
    }

    @ApiModelProperty(value = "DONE, POSSIBLE, BLOCKED or NOT_APPLICABLE", example = "BLOCKED")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @ApiModelProperty(value = "The steps this one waits for, when it is blocked")
    public List<String> getBlockedBy() {
        return blockedBy;
    }

    public void setBlockedBy(List<String> blockedBy) {
        this.blockedBy = blockedBy;
    }

    @ApiModelProperty(value = "The steps this one depends on, whatever their state: the order a diagram draws")
    public List<String> getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(List<String> dependsOn) {
        this.dependsOn = dependsOn;
    }

    @ApiModelProperty(value = "How many of the things the files name for this step exist on the instance")
    public int getDone() {
        return done;
    }

    public void setDone(int done) {
        this.done = done;
    }

    @ApiModelProperty(value = "How many the files name")
    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    @ApiModelProperty(value = "The creation that declares this step, or none when it has no creation of its own")
    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}
