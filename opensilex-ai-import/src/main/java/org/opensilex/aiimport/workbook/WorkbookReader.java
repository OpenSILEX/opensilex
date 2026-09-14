//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.workbook;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Date1904Support;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.opensilex.aiimport.exception.WorkbookReadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads an {@code .xlsx} or {@code .xls} workbook into a {@link WorkbookStructure}.
 * <p>
 * Cells are kept as text, deliberately: the point is to show the user and the assistant what was
 * actually typed, before any interpretation. {@link ExcelValueParser} does the interpreting.
 *
 * @author Arnaud Charleroy
 */
public class WorkbookReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkbookReader.class);

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * A sheet whose first rows hold at most this many filled columns is treated as prose rather
     * than as a table.
     */
    private static final int MIN_TABULAR_COLUMNS = 2;

    /**
     * Longest a cell may be and still pass for a column header. Headers are labels, even versioned
     * ones such as {@code Nouvelle_Abreviation_FR_maj_03_2023}; the lines of a ReadMe are
     * sentences. Without this bound a paragraph followed by a date-formatted cell reads as a header
     * row, and the instructions the template author wrote are lost.
     */
    private static final int MAX_HEADER_LENGTH = 80;

    /**
     * Beyond this many rows we stop reading a sheet. A field workbook holds hundreds of rows, not
     * hundreds of thousands, and this bounds the memory a single upload can claim.
     */
    private final int maxRowsPerSheet;

    public WorkbookReader() {
        this(50_000);
    }

    public WorkbookReader(int maxRowsPerSheet) {
        this.maxRowsPerSheet = maxRowsPerSheet;
    }

    public WorkbookStructure read(File file, String originalFileName) throws WorkbookReadException {
        WorkbookStructure structure = new WorkbookStructure()
                .setFileName(originalFileName)
                .setFileSizeBytes(file.length());

        try (Workbook workbook = WorkbookFactory.create(file, null, true)) {
            structure.setDate1904(usesThe1904DateSystem(workbook));
            DataFormatter formatter = new DataFormatter();
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                if (workbook.isSheetHidden(sheetIndex) || workbook.isSheetVeryHidden(sheetIndex)) {
                    continue;
                }
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                structure.getSheets().add(readSheet(sheet, sheetIndex, formatter));
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Could not read workbook {}", originalFileName, e);
            throw new WorkbookReadException(originalFileName, e);
        }

        if (structure.getSheets().isEmpty()) {
            throw new WorkbookReadException(originalFileName, new IllegalStateException("no readable sheet"));
        }
        return structure;
    }

    /**
     * POI resolves date-formatted cells itself, so this flag matters for the cells it cannot: a
     * bare number sitting in a date column. It also lets a profile tell the user their workbook is
     * not in the date system their template asks for.
     */
    private boolean usesThe1904DateSystem(Workbook workbook) {
        return workbook instanceof Date1904Support && ((Date1904Support) workbook).isDate1904();
    }

    private SheetStructure readSheet(Sheet sheet, int sheetIndex, DataFormatter formatter) {
        SheetStructure result = new SheetStructure()
                .setName(sheet.getSheetName())
                .setIndex(sheetIndex);

        List<List<String>> allRows = new ArrayList<>();
        int rowCount = 0;
        for (Row row : sheet) {
            if (rowCount++ >= maxRowsPerSheet) {
                LOGGER.warn("Sheet {} truncated at {} rows", sheet.getSheetName(), maxRowsPerSheet);
                break;
            }
            allRows.add(readRow(row, formatter));
        }
        trimTrailingEmptyRows(allRows);

        int headerRowIndex = findHeaderRow(allRows);
        if (headerRowIndex < 0) {
            // Prose sheet, such as a ReadMe: keep the text so the assistant can read the
            // instructions the author wrote for whoever fills the file.
            return result.setTabular(false).setText(joinAsText(allRows));
        }

        result.setHeaders(allRows.get(headerRowIndex));
        List<List<String>> dataRows = new ArrayList<>();
        for (int i = headerRowIndex + 1; i < allRows.size(); i++) {
            List<String> row = allRows.get(i);
            if (isEmpty(row)) {
                continue;
            }
            dataRows.add(row);
        }
        return result.setRows(dataRows);
    }

    private List<String> readRow(Row row, DataFormatter formatter) {
        List<String> values = new ArrayList<>();
        int lastCell = row.getLastCellNum();
        for (int columnIndex = 0; columnIndex < lastCell; columnIndex++) {
            Cell cell = row.getCell(columnIndex);
            values.add(cell == null ? "" : readCell(cell, formatter));
        }
        trimTrailingEmptyCells(values);
        return values;
    }

    private String readCell(Cell cell, DataFormatter formatter) {
        CellType type = cell.getCellType() == CellType.FORMULA
                ? cell.getCachedFormulaResultType()
                : cell.getCellType();

        switch (type) {
            case STRING:
                return ExcelValueParser.clean(cell.getStringCellValue());
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().format(ISO_DATE);
                }
                return formatNumber(cell.getNumericCellValue());
            case BLANK:
            case _NONE:
            case ERROR:
            default:
                return ExcelValueParser.clean(formatter.formatCellValue(cell));
        }
    }

    /**
     * Renders a number without the exponent notation and without a trailing {@code .0}, so that
     * identifiers such as a plot number stay recognisable to the user.
     */
    private String formatNumber(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value) && Math.abs(value) < 1e15) {
            return Long.toString((long) value);
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    /**
     * The header row is the first row holding at least {@value #MIN_TABULAR_COLUMNS} non-empty
     * cells that all look like labels: short, and not numbers. Field templates put title rows above
     * the header, so scanning rather than assuming row 0 matters.
     */
    private int findHeaderRow(List<List<String>> rows) {
        for (int i = 0; i < rows.size(); i++) {
            if (isHeaderRow(rows.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private boolean isHeaderRow(List<String> row) {
        int filled = 0;
        for (String value : row) {
            if (value.isEmpty()) {
                continue;
            }
            filled++;
            if (value.length() > MAX_HEADER_LENGTH || ExcelValueParser.parseNumber(value) != null) {
                return false;
            }
        }
        return filled >= MIN_TABULAR_COLUMNS;
    }

    private String joinAsText(List<List<String>> rows) {
        StringBuilder builder = new StringBuilder();
        for (List<String> row : rows) {
            String line = String.join(" ", row).trim();
            if (!line.isEmpty()) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString().trim();
    }

    private boolean isEmpty(List<String> row) {
        for (String value : row) {
            if (!value.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void trimTrailingEmptyCells(List<String> values) {
        while (!values.isEmpty() && values.get(values.size() - 1).isEmpty()) {
            values.remove(values.size() - 1);
        }
    }

    private void trimTrailingEmptyRows(List<List<String>> rows) {
        while (!rows.isEmpty() && isEmpty(rows.get(rows.size() - 1))) {
            rows.remove(rows.size() - 1);
        }
    }
}
