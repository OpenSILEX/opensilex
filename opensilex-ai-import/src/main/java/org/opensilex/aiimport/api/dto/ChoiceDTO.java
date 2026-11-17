//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * One thing the user can choose in the description of a file — a kind of sheet, a role of a column —
 * with the key its label is translated under. Served rather than written into the interface, so
 * that a kind or a role added to the module reaches the screen without a change there.
 *
 * @author Arnaud Charleroy
 */
public class ChoiceDTO {

    private String value;

    @JsonProperty("label_key")
    private String labelKey;

    /**
     * For a role, the kinds of sheet that accept it; empty for a kind.
     */
    @JsonProperty("accepted_by")
    private List<String> acceptedBy = new ArrayList<>();

    @JsonProperty("single_per_sheet")
    private boolean singlePerSheet;

    public static ChoiceDTO of(String value, String labelKey) {
        ChoiceDTO dto = new ChoiceDTO();
        dto.value = value;
        dto.labelKey = labelKey;
        return dto;
    }

    @ApiModelProperty(value = "The value to send back", example = "OBJECTS_AND_DATA")
    public String getValue() {
        return value;
    }

    @ApiModelProperty(value = "The translation key of its label", example = "AiImport.describe.kind_OBJECTS")
    public String getLabelKey() {
        return labelKey;
    }

    @ApiModelProperty(value = "For a role, the kinds of sheet that accept it")
    public List<String> getAcceptedBy() {
        return acceptedBy;
    }

    public ChoiceDTO setAcceptedBy(List<String> acceptedBy) {
        this.acceptedBy = acceptedBy;
        return this;
    }

    @ApiModelProperty(value = "For a role, whether a sheet has at most one column described so")
    public boolean isSinglePerSheet() {
        return singlePerSheet;
    }

    public ChoiceDTO setSinglePerSheet(boolean singlePerSheet) {
        this.singlePerSheet = singlePerSheet;
        return this;
    }
}
