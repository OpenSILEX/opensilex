//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.graphimport.preview.ImportPreview;

import java.util.List;

/**
 * What an import would do to the instance: its figures and the map of its sheets. Nothing is written.
 *
 * @author Arnaud Charleroy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreviewDTO {

    @JsonProperty("session_id")
    @ApiModelProperty(value = "Import identifier")
    private String sessionId;

    @JsonProperty("counts")
    @ApiModelProperty(value = "How many resources would be created, completed, left as they are, or cannot be settled")
    private PreviewCountsDTO counts;

    @JsonProperty("problem_total")
    @ApiModelProperty(value = "How many problems the file has, those about a required value excepted: the preview "
            + "settles them", example = "4")
    private int problemTotal;

    @JsonProperty("sheets")
    @ApiModelProperty(value = "The sheets, in the workbook's order")
    private List<PreviewSheetDTO> sheets;

    @JsonProperty("links")
    @ApiModelProperty(value = "How the sheets hang together")
    private List<SheetLinkDTO> links;

    @JsonProperty("data_sheets")
    @ApiModelProperty(value = "What each sheet of data gives, counted")
    private List<DataSheetDTO> dataSheets;

    public static PreviewDTO fromModel(String sessionId, ImportPreview model) {
        PreviewDTO dto = new PreviewDTO();
        dto.sessionId = sessionId;
        dto.counts = PreviewCountsDTO.fromModel(model.getCounts());
        dto.problemTotal = model.getProblemTotal();
        dto.sheets = model.getMap().sheets().stream().map(PreviewSheetDTO::fromModel).toList();
        dto.links = model.getMap().links().stream().map(SheetLinkDTO::fromModel).toList();
        dto.dataSheets = model.getData().stream().map(DataSheetDTO::fromModel).toList();
        return dto;
    }

    public List<DataSheetDTO> getDataSheets() {
        return dataSheets;
    }

    public String getSessionId() {
        return sessionId;
    }

    public PreviewCountsDTO getCounts() {
        return counts;
    }

    public int getProblemTotal() {
        return problemTotal;
    }

    public List<PreviewSheetDTO> getSheets() {
        return sheets;
    }

    public List<SheetLinkDTO> getLinks() {
        return links;
    }
}
