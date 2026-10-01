//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.create.rows.RowError;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The errors of a bulk validation, with the rows they are about.
 * <p>
 * The rows travel with the errors — their values as the workbook holds them — so the interface can
 * draw the user's own rows with the faulty cells marked, without reading the file a second time.
 * Only rows with an error are sent: a grid of five thousand correct rows would bury the forty that
 * need attention.
 *
 * @author Arnaud Charleroy
 */
public class BulkValidationDTO {

    /**
     * More than this and the errors are sampled: the first thousand show every pattern there is,
     * and the count says how many there are in all.
     */
    public static final int MAX_ERRORS = 1000;

    private String target;

    @JsonProperty("rows_checked")
    private int rowsChecked;

    @JsonProperty("rows_in_error")
    private int rowsInError;

    @JsonProperty("error_count")
    private int errorCount;

    private List<RowErrorDTO> errors = new ArrayList<>();

    /**
     * The faulty rows, per sheet, in the column order of the sheet.
     */
    private List<SheetRowsDTO> sheets = new ArrayList<>();

    public static BulkValidationDTO of(String target, int rowsChecked, List<RowError> errors,
                                       WorkbookStructure workbook) {
        BulkValidationDTO dto = new BulkValidationDTO();
        dto.target = target;
        dto.rowsChecked = rowsChecked;
        dto.errorCount = errors.size();

        Map<String, Set<Integer>> faultyRows = new LinkedHashMap<>();
        for (RowError error : errors) {
            if (!error.isAboutTheFile()) {
                faultyRows.computeIfAbsent(error.getSheet(), key -> new LinkedHashSet<>())
                        .add(error.getRow());
            }
        }
        dto.rowsInError = faultyRows.values().stream().mapToInt(Set::size).sum();

        errors.stream().limit(MAX_ERRORS).forEach(error -> dto.errors.add(RowErrorDTO.fromModel(error)));
        faultyRows.forEach((sheet, rows) -> dto.sheets.add(SheetRowsDTO.of(sheet, rows, workbook)));
        return dto;
    }

    @ApiModelProperty(value = "What was validated", example = "DATA")
    public String getTarget() {
        return target;
    }

    @ApiModelProperty(value = "How many rows of the workbook were checked")
    public int getRowsChecked() {
        return rowsChecked;
    }

    @ApiModelProperty(value = "How many distinct rows have at least one error")
    public int getRowsInError() {
        return rowsInError;
    }

    @ApiModelProperty(value = "How many errors in all; at most " + MAX_ERRORS + " are listed")
    public int getErrorCount() {
        return errorCount;
    }

    public List<RowErrorDTO> getErrors() {
        return errors;
    }

    public List<SheetRowsDTO> getSheets() {
        return sheets;
    }

    /**
     * One error, where the user will look for it.
     */
    public static class RowErrorDTO {

        private String sheet;
        private int row;
        private String column;
        private String value;
        private String kind;
        private ReportMessageDTO message;

        static RowErrorDTO fromModel(RowError model) {
            RowErrorDTO dto = new RowErrorDTO();
            dto.sheet = model.getSheet();
            dto.row = model.getRow();
            dto.column = model.getColumn();
            dto.value = model.getValue();
            dto.kind = model.getKind().name();
            dto.message = ReportMessageDTO.fromModel(model.getMessage());
            return dto;
        }

        @ApiModelProperty(value = "Sheet of the workbook; absent for an error about the whole file")
        public String getSheet() {
            return sheet;
        }

        @ApiModelProperty(value = "Row number as the spreadsheet shows it; 0 for the whole file")
        public int getRow() {
            return row;
        }

        @ApiModelProperty(value = "Column header as the workbook writes it")
        public String getColumn() {
            return column;
        }

        public String getValue() {
            return value;
        }

        @ApiModelProperty(example = "MISSING_VALUE")
        public String getKind() {
            return kind;
        }

        public ReportMessageDTO getMessage() {
            return message;
        }
    }

    /**
     * The faulty rows of one sheet, with the headers that give their columns an order.
     */
    public static class SheetRowsDTO {

        private String sheet;
        private List<String> headers = new ArrayList<>();
        private List<RowValuesDTO> rows = new ArrayList<>();

        /**
         * The profiles number a data row as its index in the sheet plus two — the header is row 1
         * — so the same convention reads it back here.
         */
        static SheetRowsDTO of(String name, Set<Integer> rowNumbers, WorkbookStructure workbook) {
            SheetRowsDTO dto = new SheetRowsDTO();
            dto.sheet = name;
            Optional<SheetStructure> sheet = workbook == null ? Optional.empty() : workbook.getSheet(name);
            if (!sheet.isPresent()) {
                rowNumbers.forEach(number -> dto.rows.add(new RowValuesDTO(number, new LinkedHashMap<>())));
                return dto;
            }
            dto.headers.addAll(sheet.get().getHeaders());
            for (int number : rowNumbers) {
                Map<String, String> values = new LinkedHashMap<>();
                int index = number - 2;
                if (index >= 0 && index < sheet.get().getRows().size()) {
                    List<String> row = sheet.get().getRows().get(index);
                    for (String header : dto.headers) {
                        values.put(header, sheet.get().cell(row, header));
                    }
                }
                dto.rows.add(new RowValuesDTO(number, values));
            }
            return dto;
        }

        public String getSheet() {
            return sheet;
        }

        public List<String> getHeaders() {
            return headers;
        }

        public List<RowValuesDTO> getRows() {
            return rows;
        }
    }

    /**
     * One row, with its values by header.
     */
    public static class RowValuesDTO {

        private final int row;
        private final Map<String, String> values;

        RowValuesDTO(int row, Map<String, String> values) {
            this.row = row;
            this.values = values;
        }

        public int getRow() {
            return row;
        }

        public Map<String, String> getValues() {
            return values;
        }
    }
}
