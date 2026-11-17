//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.api.dto.ReportMessageDTO;
import org.opensilex.graphimport.interpret.Choice;

import java.net.URI;
import java.util.List;

/**
 * What was settled for one question, how, and what else was possible.
 *
 * @author Arnaud Charleroy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChoiceDTO {

    @JsonProperty("chosen_uri")
    @ApiModelProperty(value = "The property or class settled; absent while undecided")
    private URI chosenUri;

    @JsonProperty("chosen_column")
    @ApiModelProperty(value = "The identifier column settled, from 0; absent while undecided")
    private Integer chosenColumn;

    @JsonProperty("status")
    @ApiModelProperty(value = "CHOSEN by the user, DECIDED by the file, SUGGESTED by the evidence, or UNDECIDED",
            example = "SUGGESTED")
    private String status;

    @JsonProperty("candidates")
    @ApiModelProperty(value = "The answers considered, best first")
    private List<CandidateDTO> candidates;

    @JsonProperty("notes")
    @ApiModelProperty(value = "What the user should know; keys go under GraphImport.report.")
    private List<ReportMessageDTO> notes;

    static ChoiceDTO ofUri(Choice<URI> choice, List<CandidateDTO> candidates) {
        ChoiceDTO dto = of(choice, candidates);
        dto.chosenUri = choice.chosen();
        return dto;
    }

    static ChoiceDTO ofColumn(Choice<Integer> choice, List<CandidateDTO> candidates) {
        ChoiceDTO dto = of(choice, candidates);
        dto.chosenColumn = choice.chosen();
        return dto;
    }

    private static ChoiceDTO of(Choice<?> choice, List<CandidateDTO> candidates) {
        ChoiceDTO dto = new ChoiceDTO();
        dto.status = choice.status().name();
        dto.candidates = candidates;
        dto.notes = ReportMessageDTO.fromModels(choice.notes());
        return dto;
    }

    public URI getChosenUri() {
        return chosenUri;
    }

    public Integer getChosenColumn() {
        return chosenColumn;
    }

    public String getStatus() {
        return status;
    }

    public List<CandidateDTO> getCandidates() {
        return candidates;
    }

    public List<ReportMessageDTO> getNotes() {
        return notes;
    }
}
