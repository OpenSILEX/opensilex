//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * What the user changes in the description of their files: the experiment, and the sheets they
 * touched. Everything is applied together, or nothing is.
 *
 * @author Arnaud Charleroy
 */
public class FileDescriptionUpdateDTO {

    @JsonProperty("session_id")
    private String sessionId;

    private URI experiment;

    private Boolean remember;

    @Valid
    private List<SheetDescriptionUpdateDTO> sheets = new ArrayList<>();

    @NotNull
    @ApiModelProperty(value = "The conversation whose files are described", required = true)
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @ApiModelProperty(value = "Whether to keep what is said of the sheets as a template for the next files of the "
            + "same shape: yes unless said otherwise")
    public Boolean getRemember() {
        return remember;
    }

    public void setRemember(Boolean remember) {
        this.remember = remember;
    }

    /**
     * @return whether the description is to be remembered, which it is unless the user said not
     */
    public boolean remembers() {
        return remember == null || remember;
    }

    @ApiModelProperty(value = "The experiment the files belong to, when no column names it")
    public URI getExperiment() {
        return experiment;
    }

    public void setExperiment(URI experiment) {
        this.experiment = experiment;
    }

    @ApiModelProperty(value = "The sheets that change")
    public List<SheetDescriptionUpdateDTO> getSheets() {
        return sheets;
    }

    public void setSheets(List<SheetDescriptionUpdateDTO> sheets) {
        this.sheets = sheets;
    }
}
