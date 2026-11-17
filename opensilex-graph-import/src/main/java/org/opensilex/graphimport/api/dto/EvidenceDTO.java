//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.api.dto.ReportMessageDTO;
import org.opensilex.graphimport.interpret.Evidence;

/**
 * One reason behind a suggestion, with how much it counts.
 *
 * @author Arnaud Charleroy
 */
public class EvidenceDTO {

    @JsonProperty("source")
    @ApiModelProperty(value = "The source that found it", example = "label")
    private String source;

    @JsonProperty("weight")
    @ApiModelProperty(value = "How much it counts: positive for, negative against", example = "0.7")
    private double weight;

    @JsonProperty("reason")
    @ApiModelProperty(value = "The sentence shown to the user; its key goes under GraphImport.report.")
    private ReportMessageDTO reason;

    public static EvidenceDTO fromModel(Evidence evidence) {
        EvidenceDTO dto = new EvidenceDTO();
        dto.source = evidence.source();
        dto.weight = evidence.weight();
        dto.reason = ReportMessageDTO.fromModel(evidence.reason());
        return dto;
    }

    public String getSource() {
        return source;
    }

    public double getWeight() {
        return weight;
    }

    public ReportMessageDTO getReason() {
        return reason;
    }
}
