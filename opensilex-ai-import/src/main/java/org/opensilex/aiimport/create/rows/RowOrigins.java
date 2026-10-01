//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.rows;

import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Where each line of a generated CSV came from in the user's workbook.
 * <p>
 * Filled while the CSV is written, one entry per data row in the order they are written, and one
 * per generated column. Read back when the platform's validation answers, to turn its row and
 * header into the sheet, row and column the user knows.
 * <p>
 * Columns are matched on the generated <em>header</em>, never on its index: the platform's CSV
 * engine counts columns from 0 for some errors and from 1 for others, while the header it reports
 * is always the one that was written.
 *
 * @author Arnaud Charleroy
 */
public class RowOrigins {

    /**
     * Joins a sheet and a generated header into one key; a character no header carries.
     */
    private static final String SHEET_SEPARATOR = "\u0000";

    private final List<Origin> rows = new ArrayList<>();
    private final Map<String, String> headers = new HashMap<>();

    /**
     * Records the next data row of the CSV.
     *
     * @return its index in the body of the CSV, from 0
     */
    public int addRow(String sheet, int row) {
        rows.add(new Origin(sheet, row));
        return rows.size() - 1;
    }

    /**
     * Records that a generated column holds what the workbook calls {@code workbookHeader}.
     */
    public RowOrigins addColumn(String generatedHeader, String workbookHeader) {
        headers.put(key(generatedHeader), workbookHeader);
        return this;
    }

    /**
     * Records the workbook header of a generated column for one sheet only: two sheets can feed the
     * same property from columns they name differently.
     */
    public RowOrigins addColumn(String sheet, String generatedHeader, String workbookHeader) {
        headers.putIfAbsent(sheet + SHEET_SEPARATOR + key(generatedHeader), workbookHeader);
        return this;
    }

    /**
     * @return the header the given sheet uses for a generated column, else what
     *         {@link #workbookHeaderOf(String)} says
     */
    public String workbookHeaderOf(String sheet, String generatedHeader) {
        String own = sheet == null ? null : headers.get(sheet + SHEET_SEPARATOR + key(generatedHeader));
        return own != null ? own : workbookHeaderOf(generatedHeader);
    }

    /**
     * A property header in its expanded form: the module writes full URIs, and the platform
     * reports some of them back prefixed — {@code vocabulary:hasCreationDate} for the column that
     * was written {@code http://www.opensilex.org/vocabulary/oeso#hasCreationDate}.
     */
    private static String key(String header) {
        if (header == null || !header.contains(":")) {
            return header;
        }
        try {
            return SPARQLDeserializers.getExpandedURI(header);
        } catch (RuntimeException e) {
            return header;
        }
    }

    public Optional<Origin> ofBodyRow(int bodyIndex) {
        return bodyIndex >= 0 && bodyIndex < rows.size()
                ? Optional.of(rows.get(bodyIndex))
                : Optional.empty();
    }

    /**
     * @return the workbook's header for a generated one, or the generated one when it has no
     *         counterpart — a column the module added, such as a type chosen by the user
     */
    public String workbookHeaderOf(String generatedHeader) {
        return headers.getOrDefault(key(generatedHeader), generatedHeader);
    }

    public int size() {
        return rows.size();
    }

    /**
     * A row of the workbook.
     */
    public static final class Origin {

        private final String sheet;
        private final int row;

        public Origin(String sheet, int row) {
            this.sheet = sheet;
            this.row = row;
        }

        public String getSheet() {
            return sheet;
        }

        /**
         * The row number as the spreadsheet shows it.
         */
        public int getRow() {
            return row;
        }
    }
}
