//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.bulk;

import org.opensilex.aiimport.create.rows.RowError;
import org.opensilex.aiimport.create.rows.RowOrigins;

import java.util.ArrayList;
import java.util.List;

/**
 * A CSV in the platform's format, written from the workbook, with where each line came from and
 * what the module already knows is wrong before asking the platform anything.
 *
 * @author Arnaud Charleroy
 */
public class GeneratedCsv {

    private final List<List<String>> lines = new ArrayList<>();
    private final RowOrigins origins = new RowOrigins();
    private final List<RowError> moduleErrors = new ArrayList<>();
    private final int headerLines;

    /**
     * @param headerLines how many lines precede the data in this platform format
     */
    public GeneratedCsv(int headerLines) {
        this.headerLines = headerLines;
    }

    public GeneratedCsv addHeaderLine(List<String> cells) {
        lines.add(cells);
        return this;
    }

    /**
     * Adds a data line and records the workbook row it came from.
     */
    public GeneratedCsv addDataLine(String sheet, int row, List<String> cells) {
        lines.add(cells);
        origins.addRow(sheet, row);
        return this;
    }

    public RowOrigins getOrigins() {
        return origins;
    }

    public List<RowError> getModuleErrors() {
        return moduleErrors;
    }

    public int getDataLineCount() {
        return lines.size() - headerLines;
    }

    /**
     * Every cell quoted: a comma or a quote in a spreadsheet cell stays inside its cell.
     */
    public String render() {
        return render(0, getDataLineCount());
    }

    /**
     * The header lines, then the data lines from {@code fromDataLine} (inclusive) to
     * {@code toDataLine} (exclusive): one batch of a file larger than the platform imports at once.
     * The batch counts its rows from 0 again; {@code fromDataLine} is what brings them back to the
     * workbook.
     */
    public String render(int fromDataLine, int toDataLine) {
        StringBuilder csv = new StringBuilder();
        for (int i = 0; i < Math.min(headerLines, lines.size()); i++) {
            append(csv, lines.get(i));
        }
        for (int i = headerLines + Math.max(0, fromDataLine);
             i < Math.min(lines.size(), headerLines + toDataLine); i++) {
            append(csv, lines.get(i));
        }
        return csv.toString();
    }

    private static void append(StringBuilder csv, List<String> line) {
        for (int i = 0; i < line.size(); i++) {
            if (i > 0) {
                csv.append(',');
            }
            String cell = line.get(i) == null ? "" : line.get(i);
            csv.append('"').append(cell.replace("\"", "\"\"")).append('"');
        }
        csv.append('\n');
    }
}
