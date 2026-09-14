//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.create.UnresolvedRow;

import java.util.ArrayList;
import java.util.List;

/**
 * One row the insertion could not resolve, named precisely enough for the user to find it in their
 * spreadsheet: the sheet, the row number as the spreadsheet shows it, and what was expected.
 *
 * @author Arnaud Charleroy
 */
public class UnresolvedRowDTO {

    private String sheet;

    private int row;

    private String column;

    /**
     * A translation key, so the interface says it in the user's language.
     */
    @JsonProperty("reason_key")
    private String reasonKey;

    private String value;

    public static UnresolvedRowDTO fromModel(UnresolvedRow model) {
        return new UnresolvedRowDTO()
                .setSheet(model.getSheet())
                .setRow(model.getRowNumber())
                .setColumn(model.getColumn())
                .setReasonKey(model.getReasonKey())
                .setValue(model.getValue());
    }

    public static List<UnresolvedRowDTO> fromModels(List<UnresolvedRow> models) {
        List<UnresolvedRowDTO> dtos = new ArrayList<>(models.size());
        models.forEach(model -> dtos.add(fromModel(model)));
        return dtos;
    }

    @ApiModelProperty(example = "data_placette")
    public String getSheet() {
        return sheet;
    }

    public UnresolvedRowDTO setSheet(String sheet) {
        this.sheet = sheet;
        return this;
    }

    @ApiModelProperty(value = "The row number as the spreadsheet numbers it", example = "42")
    public int getRow() {
        return row;
    }

    public UnresolvedRowDTO setRow(int row) {
        this.row = row;
        return this;
    }

    @ApiModelProperty(example = "plot_id")
    public String getColumn() {
        return column;
    }

    public UnresolvedRowDTO setColumn(String column) {
        this.column = column;
        return this;
    }

    @ApiModelProperty(example = "AiImport.proposal.unresolved.object")
    public String getReasonKey() {
        return reasonKey;
    }

    public UnresolvedRowDTO setReasonKey(String reasonKey) {
        this.reasonKey = reasonKey;
        return this;
    }

    @ApiModelProperty(example = "P-12")
    public String getValue() {
        return value;
    }

    public UnresolvedRowDTO setValue(String value) {
        this.value = value;
        return this;
    }
}
