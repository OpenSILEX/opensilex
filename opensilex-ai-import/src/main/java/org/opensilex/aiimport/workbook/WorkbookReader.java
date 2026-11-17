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
public class WorkbookReader implements SheetSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkbookReader.class);

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final List<String> EXTENSIONS = List.of("xlsx", "xlsm", "xls");

    /**
     * Beyond this many rows we stop reading a sheet. A field workbook holds hundreds of rows, not
     * hundreds of thousands, and this bounds the memory a single upload can claim.
     */
    public static final int DEFAULT_MAX_ROWS = 50_000;

    private final int maxRowsPerSheet;

    public WorkbookReader() {
        this(DEFAULT_MAX_ROWS);
    }

    public WorkbookReader(int maxRowsPerSheet) {
        this.maxRowsPerSheet = maxRowsPerSheet;
    }

    @Override
    public List<String> extensions() {
        return EXTENSIONS;
    }

    @Override
    public WorkbookStructure read(File file, String originalFileName) throws WorkbookReadException {
        WorkbookStructure structure = new WorkbookStructure()
                .setFileName(originalFileName)
                .setFileSizeBytes(file.length());

        try (Workbook workbook = WorkbookFactory.create(file, null, true)) {
            boolean date1904 = usesThe1904DateSystem(workbook);
            structure.setDate1904(date1904);
            DataFormatter formatter = new DataFormatter();
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                if (workbook.isSheetHidden(sheetIndex) || workbook.isSheetVeryHidden(sheetIndex)) {
                    continue;
                }
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                structure.getSheets().add(readSheet(sheet, sheetIndex, formatter, date1904));
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

    private SheetStructure readSheet(Sheet sheet, int sheetIndex, DataFormatter formatter, boolean date1904) {
        List<List<String>> allRows = new ArrayList<>();
        int rowCount = 0;
        for (Row row : sheet) {
            if (rowCount++ >= maxRowsPerSheet) {
                LOGGER.warn("Sheet {} truncated at {} rows", sheet.getSheetName(), maxRowsPerSheet);
                break;
            }
            allRows.add(readRow(row, formatter));
        }
        return HeaderRows.sheetOf(sheet.getSheetName(), sheetIndex, allRows).setDate1904(date1904);
    }

    private List<String> readRow(Row row, DataFormatter formatter) {
        List<String> values = new ArrayList<>();
        int lastCell = row.getLastCellNum();
        for (int columnIndex = 0; columnIndex < lastCell; columnIndex++) {
            Cell cell = row.getCell(columnIndex);
            values.add(cell == null ? "" : readCell(cell, formatter));
        }
        HeaderRows.trimTrailingEmptyCells(values);
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
}
