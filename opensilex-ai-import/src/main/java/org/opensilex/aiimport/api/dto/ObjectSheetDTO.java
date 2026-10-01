//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.create.objects.ObjectSheet;
import org.opensilex.aiimport.profile.ObjectRow;
import org.opensilex.aiimport.report.ReportMessage;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One object sheet as the interface draws it: the rows that would become objects, the type they
 * would take, and what each column would become — with what is wrong with that mapping.
 *
 * @author Arnaud Charleroy
 */
public class ObjectSheetDTO {

    /**
     * The rows shown; the count says how many there are in all. A preview of a few hundred shows
     * what the columns hold without shipping a whole trial to the browser.
     */
    public static final int MAX_ROWS = 300;

    private String sheet;

    @JsonProperty("name_column")
    private String nameColumn;

    private List<String> headers = new ArrayList<>();

    @JsonProperty("row_count")
    private int rowCount;

    private List<BulkValidationDTO.RowValuesDTO> rows = new ArrayList<>();

    private URI type;

    @JsonProperty("type_from_file")
    private boolean typeFromFile;

    private boolean included;

    private Map<String, String> mapping = new LinkedHashMap<>();

    private List<ReportMessageDTO> problems = new ArrayList<>();

    public static ObjectSheetDTO fromModel(ObjectSheet model, List<ReportMessage> problems) {
        ObjectSheetDTO dto = new ObjectSheetDTO();
        dto.sheet = model.getName();
        dto.nameColumn = model.getNameColumn();
        dto.headers.addAll(model.getHeaders());
        dto.rowCount = model.getRows().size();
        for (ObjectRow row : model.getRows().subList(0, Math.min(MAX_ROWS, model.getRows().size()))) {
            dto.rows.add(new BulkValidationDTO.RowValuesDTO(row.getRow(), new LinkedHashMap<>(row.getCells())));
        }
        dto.type = model.getType();
        dto.typeFromFile = model.isTypeFromFile();
        dto.included = model.isIncluded();
        dto.mapping.putAll(model.getMapping());
        dto.problems.addAll(ReportMessageDTO.fromModels(problems));
        return dto;
    }

    @ApiModelProperty(value = "The sheet's name", example = "ed_placette")
    public String getSheet() {
        return sheet;
    }

    @ApiModelProperty(value = "The column naming the objects; always written as their name", example = "plot_id")
    public String getNameColumn() {
        return nameColumn;
    }

    public List<String> getHeaders() {
        return headers;
    }

    @ApiModelProperty(value = "How many objects the sheet lists; at most " + MAX_ROWS + " rows are sent")
    public int getRowCount() {
        return rowCount;
    }

    public List<BulkValidationDTO.RowValuesDTO> getRows() {
        return rows;
    }

    @ApiModelProperty(value = "The type the objects would take: chosen, or stated by the file")
    public URI getType() {
        return type;
    }

    @ApiModelProperty(value = "Whether the type is the file's own, not yet chosen by anyone")
    public boolean isTypeFromFile() {
        return typeFromFile;
    }

    @ApiModelProperty(value = "Whether the sheet takes part in the next creation")
    public boolean isIncluded() {
        return included;
    }

    @ApiModelProperty(value = "Every column, with the property it becomes; empty for a column not written, "
            + "'x' and 'y' for a position")
    public Map<String, String> getMapping() {
        return mapping;
    }

    @ApiModelProperty(value = "What the type chosen contradicts in the mapping")
    public List<ReportMessageDTO> getProblems() {
        return problems;
    }
}
