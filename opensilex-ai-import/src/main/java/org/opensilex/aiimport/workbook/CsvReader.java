//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.workbook;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import org.apache.commons.io.FilenameUtils;
import org.opensilex.aiimport.exception.WorkbookReadException;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a CSV file as a workbook of one sheet, named after the file.
 * <p>
 * The separator, the quotes and the line endings are detected by the platform's own CSV settings,
 * so a file comes in the way every other CSV import of the platform reads it. Cells are kept as
 * text, as for a workbook: a comma decimal stays a comma until {@link ExcelValueParser} reads it.
 *
 * @author Arnaud Charleroy
 */
public class CsvReader implements SheetSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsvReader.class);

    /**
     * What a file written by a spreadsheet on Windows is in when it is not UTF-8.
     */
    public static final Charset FALLBACK_CHARSET = Charset.forName("windows-1252");

    private static final List<String> EXTENSIONS = List.of("csv", "tsv");
    private static final int SEPARATOR_SAMPLE_ROWS = 20;
    private static final int MAX_CHARS_PER_CELL = 32_768;
    private static final char BYTE_ORDER_MARK = '﻿';

    private final int maxRowsPerSheet;

    public CsvReader() {
        this(WorkbookReader.DEFAULT_MAX_ROWS);
    }

    public CsvReader(int maxRowsPerSheet) {
        this.maxRowsPerSheet = maxRowsPerSheet;
    }

    @Override
    public List<String> extensions() {
        return EXTENSIONS;
    }

    @Override
    public WorkbookStructure read(File file, String originalFileName) throws WorkbookReadException {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            Charset charset = StandardCharsets.UTF_8;
            String text = decode(bytes, StandardCharsets.UTF_8);
            if (text == null) {
                charset = FALLBACK_CHARSET;
                text = new String(bytes, FALLBACK_CHARSET);
            }
            if (!text.isEmpty() && text.charAt(0) == BYTE_ORDER_MARK) {
                text = text.substring(1);
            }

            SheetStructure sheet = HeaderRows.sheetOf(FilenameUtils.getBaseName(originalFileName), 0, rowsOf(text))
                    .setCharset(charset == StandardCharsets.UTF_8 ? null : charset.name());
            return new WorkbookStructure()
                    .setFileName(originalFileName)
                    .setFileSizeBytes(file.length())
                    .setSheets(new ArrayList<>(List.of(sheet)));
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Could not read CSV file {}", originalFileName, e);
            throw new WorkbookReadException(originalFileName, e);
        }
    }

    /**
     * @return the text, or {@code null} when the bytes are not valid in this charset: a file is
     * either read exactly or read in the fallback, never half decoded with replacement characters
     */
    private static String decode(byte[] bytes, Charset charset) {
        try {
            return charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    private List<List<String>> rowsOf(String text) {
        CsvParserSettings settings = ClassUtils.getCSVParserDefaultSettings();
        settings.setFormatDetectorRowSampleCount(SEPARATOR_SAMPLE_ROWS);
        settings.setMaxCharsPerColumn(MAX_CHARS_PER_CELL);
        settings.setNumberOfRecordsToRead(maxRowsPerSheet);
        CsvParser parser = new CsvParser(settings);

        List<List<String>> rows = new ArrayList<>();
        for (String[] record : parser.parseAll(new StringReader(text))) {
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
