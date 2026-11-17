//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.workbook;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.opensilex.aiimport.workbook.CsvReader;
import org.opensilex.aiimport.workbook.ExcelValueParser;
import org.opensilex.aiimport.workbook.HeaderRows;
import org.opensilex.aiimport.workbook.WorkbookReader;
import org.opensilex.utils.ClassUtils;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads every row of every sheet as it is in the file, the rows above the header and the empty ones
 * included, so that the user can say which row is the header and where the values start.
 * <p>
 * Row {@code i} of a sheet is row {@code i + 1} of the spreadsheet: an empty row Excel does not even
 * store still takes its place. Cells are written as the platform's readers write them (dates in ISO
 * form, whole numbers without {@code .0}, invisible characters removed), so that a sheet laid out by
 * hand reads like a sheet laid out by the reader. Hidden sheets are left out, as the reader does.
 *
 * @author Arnaud Charleroy
 */
public final class RawRows {

    private static final char BYTE_ORDER_MARK = '﻿';
    private static final int SEPARATOR_SAMPLE_ROWS = 20;
    private static final int MAX_CHARS_PER_CELL = 32_768;
    private static final double MAX_PLAIN_INTEGER = 1e15;

    private RawRows() {
    }

    /** One sheet's rows, as written in the file. */
    public record RawSheet(String name, List<List<String>> rows) {
    }

    /**
     * @param fileName the name the user gave the file, whose extension tells how to read it
     */
    public static List<RawSheet> read(File file, String fileName) throws IOException {
        String extension = FilenameUtils.getExtension(fileName).toLowerCase(Locale.ROOT);
        if ("csv".equals(extension) || "tsv".equals(extension)) {
            return List.of(new RawSheet(FilenameUtils.getBaseName(fileName), csvRows(file)));
        }
        return workbookRows(file);
    }

    private static List<RawSheet> workbookRows(File file) throws IOException {
        List<RawSheet> sheets = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file, null, true)) {
            DataFormatter formatter = new DataFormatter();
            for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
                if (!workbook.isSheetHidden(index) && !workbook.isSheetVeryHidden(index)) {
                    Sheet sheet = workbook.getSheetAt(index);
                    sheets.add(new RawSheet(sheet.getSheetName(), sheetRows(sheet, formatter)));
                }
            }
        }
        return sheets;
    }

    private static List<List<String>> sheetRows(Sheet sheet, DataFormatter formatter) {
        List<List<String>> rows = new ArrayList<>();
        for (Row row : sheet) {
            if (row.getRowNum() >= WorkbookReader.DEFAULT_MAX_ROWS) {
                break;
            }
            while (rows.size() < row.getRowNum()) {
                rows.add(new ArrayList<>());
            }
            List<String> cells = new ArrayList<>();
            for (int column = 0; column < row.getLastCellNum(); column++) {
                Cell cell = row.getCell(column);
                cells.add(cell == null ? "" : cell(cell, formatter));
            }
            HeaderRows.trimTrailingEmptyCells(cells);
            rows.add(cells);
        }
        return rows;
    }

    private static String cell(Cell cell, DataFormatter formatter) {
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        switch (type) {
            case STRING:
                return ExcelValueParser.clean(cell.getStringCellValue());
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().format(DateTimeFormatter.ISO_DATE);
                }
                return number(cell.getNumericCellValue());
            default:
                return ExcelValueParser.clean(formatter.formatCellValue(cell));
        }
    }

    private static String number(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value) && Math.abs(value) < MAX_PLAIN_INTEGER) {
            return Long.toString((long) value);
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static List<List<String>> csvRows(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            text = new String(bytes, CsvReader.FALLBACK_CHARSET);
        }
        if (!text.isEmpty() && text.charAt(0) == BYTE_ORDER_MARK) {
            text = text.substring(1);
        }

        CsvParserSettings settings = ClassUtils.getCSVParserDefaultSettings();
        settings.setFormatDetectorRowSampleCount(SEPARATOR_SAMPLE_ROWS);
        settings.setMaxCharsPerColumn(MAX_CHARS_PER_CELL);
        settings.setNumberOfRecordsToRead(WorkbookReader.DEFAULT_MAX_ROWS);
        settings.setSkipEmptyLines(false);
        List<List<String>> rows = new ArrayList<>();
        for (String[] record : new CsvParser(settings).parseAll(new StringReader(text))) {
            List<String> row = new ArrayList<>();
            for (String cell : record) {
                row.add(cell == null ? "" : ExcelValueParser.clean(cell));
            }
            HeaderRows.trimTrailingEmptyCells(row);
            rows.add(row);
        }
        return rows;
    }
}
