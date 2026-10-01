//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.export;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * How the STAR standard describes its metadata columns, so an exported workbook describes them the
 * same way the reference template does.
 * <p>
 * Read from {@value #RESOURCE}, a copy of the template's {@code dictionary_metadata}, rather than
 * written again here: a description reworded by this module would be a second standard. The
 * columns this module adds — the URIs of origin, the type of the objects — are declared in the same
 * file, without a URI, since the standard has none for them.
 *
 * @author Arnaud Charleroy
 */
public final class StarMetadataDescriptions {

    public static final String RESOURCE = "/star/dictionary_metadata.tsv";

    /**
     * One column as the standard describes it.
     */
    public record Entry(String name, String description, String unit, String rClass, String uri,
                        String eloaClass) {
    }

    private static final Map<String, Entry> BY_NAME = load();

    private StarMetadataDescriptions() {
    }

    public static Optional<Entry> get(String column) {
        return column == null
                ? Optional.empty()
                : Optional.ofNullable(BY_NAME.get(column.toLowerCase(Locale.ROOT)));
    }

    private static Map<String, Entry> load() {
        Map<String, Entry> entries = new LinkedHashMap<>();
        try (InputStream in = StarMetadataDescriptions.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                return Collections.emptyMap();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] cells = line.split("\t", -1);
                Entry entry = new Entry(cell(cells, 0), cell(cells, 1), cell(cells, 2), cell(cells, 3),
                        cell(cells, 4), cell(cells, 5));
                entries.put(entry.name().toLowerCase(Locale.ROOT), entry);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return Collections.unmodifiableMap(entries);
    }

    private static String cell(String[] cells, int index) {
        return index < cells.length ? cells[index].trim() : "";
    }
}
