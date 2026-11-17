//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.session;

import org.opensilex.aiimport.exception.WorkbookReadException;
import org.opensilex.aiimport.service.SessionFile;
import org.opensilex.aiimport.workbook.SheetSources;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.graphimport.session.store.GraphImportSnapshot;
import org.opensilex.graphimport.session.store.SnapshotSessionStore;
import org.opensilex.graphimport.workbook.RawRows;
import org.opensilex.graphimport.workbook.SheetLayout;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

/**
 * Lets the user say which row of a sheet is the header and where its values start, when the reader
 * guessed wrong: a title above a header it did not recognise, a row of units below it.
 * <p>
 * The rows are read again from the stored copy of the file, since the reader keeps neither the rows
 * above the header nor their numbers.
 *
 * @author Arnaud Charleroy
 */
public final class SheetLayouts {

    /** How many rows of a sheet the user sees to choose from, at least. */
    public static final int ROWS_SHOWN = 30;

    /** Rows shown below the first data row, so that the user sees the values the choice gives. */
    private static final int ROWS_BELOW = 5;

    private SheetLayouts() {
    }

    /**
     * The first rows of a sheet, as the file holds them, with its current and its detected layout.
     *
     * @param location where the sheet comes from
     * @param current  the layout in use: the user's, else the detected one; null when the reader saw
     *                 no table and the user chose nothing
     * @param detected the layout the reader found; null when it saw no table
     * @param rows     the first rows, row {@code i} being row {@code i + 1} of the spreadsheet
     * @param total    how many rows the sheet has
     */
    public record SheetRows(GraphImportSession.SheetLocation location, SheetLayout current, SheetLayout detected,
                            List<List<String>> rows, int total) {
    }

    /**
     * @param sheet the sheet's name in the assembled workbook
     * @return empty when the import has no such sheet
     * @throws IOException when the stored copy of its file cannot be read
     */
    public static Optional<SheetRows> rows(SnapshotSessionStore<GraphImportSnapshot> store, GraphImportSession session,
                                           String sheet) throws IOException {
        Optional<GraphImportSession.SheetLocation> location = session.locate(sheet);
        if (location.isEmpty()) {
            return Optional.empty();
        }
        SessionFile file = location.get().file();
        Stored stored = readStored(store, session.getId(), file, location.get().sheetName());
        List<List<String>> rows = stored.rows();
        SheetLayout detected = SheetLayout.detectedIn(stored.read(), rows).orElse(null);
        SheetLayout current = session.layoutOf(file.getId(), location.get().sheetName()).orElse(detected);
        int shown = Math.max(ROWS_SHOWN, current == null ? 0 : current.firstDataRow() + ROWS_BELOW);
        return Optional.of(new SheetRows(location.get(), current, detected,
                rows.subList(0, Math.min(shown, rows.size())), rows.size()));
    }

    /**
     * Lays a sheet out as the user said.
     *
     * @return why the layout does not fit the sheet, or empty once it is applied
     * @throws IOException when the stored copy of its file cannot be read
     */
    public static Optional<String> apply(SnapshotSessionStore<GraphImportSnapshot> store, GraphImportSession session,
                                         GraphImportSession.SheetLocation location, SheetLayout layout)
            throws IOException {
        SessionFile file = location.file();
        Stored stored = readStored(store, session.getId(), file, location.sheetName());
        Optional<String> unfit = layout.whyNotFor(stored.rows().size());
        if (unfit.isEmpty()) {
            session.lay(file.getId(), location.sheetName(), layout, layout.applyTo(stored.read(), stored.rows()));
        }
        return unfit;
    }

    /**
     * Gives a sheet back the layout the reader found, a sheet of text included.
     *
     * @throws IOException when the stored copy of its file cannot be read
     */
    public static void reset(SnapshotSessionStore<GraphImportSnapshot> store, GraphImportSession session,
                             GraphImportSession.SheetLocation location) throws IOException {
        SessionFile file = location.file();
        session.unlay(file.getId(), location.sheetName(),
                readStored(store, session.getId(), file, location.sheetName()).read());
    }

    /**
     * Applies again, to a file just read back from the store, the layouts chosen for its sheets. A
     * layout that no longer fits, or names a sheet the file no longer has, is dropped.
     */
    static void reapply(GraphImportSession session, SessionFile file, File copy,
                        List<GraphImportSnapshot.StoredLayout> layouts) throws IOException {
        List<RawRows.RawSheet> sheets = RawRows.read(copy, file.getName());
        for (GraphImportSnapshot.StoredLayout stored : layouts) {
            SheetLayout layout = new SheetLayout(stored.getHeaderRow(), stored.getFirstDataRow());
            Optional<RawRows.RawSheet> raw = sheets.stream().filter(sheet -> sheet.name().equals(stored.getSheet()))
                    .findFirst();
            SessionFile current = session.getFile(file.getId()).orElse(file);
            if (raw.isPresent() && layout.whyNotFor(raw.get().rows().size()).isEmpty()) {
                session.lay(file.getId(), stored.getSheet(), layout,
                        layout.applyTo(sheetOf(current, stored.getSheet()), raw.get().rows()));
            }
        }
    }

    /**
     * A sheet read from the stored copy of its file twice: its raw rows, and the sheet as the reader
     * makes it, which keeps telling what the reader detected whatever layout the user chose since.
     */
    private record Stored(List<List<String>> rows, SheetStructure read) {
    }

    private static Stored readStored(SnapshotSessionStore<GraphImportSnapshot> store, String sessionId,
                                     SessionFile file, String sheetName) throws IOException {
        File copy = SessionFiles.temporaryCopy(file.getName(), store.readFile(sessionId, file.getId()));
        try {
            List<List<String>> rows = RawRows.read(copy, file.getName()).stream()
                    .filter(sheet -> sheet.name().equals(sheetName))
                    .findFirst()
                    .map(RawRows.RawSheet::rows)
                    .orElseThrow(() -> new IOException("The stored file has no sheet " + sheetName));
            SheetStructure read = SheetSources.read(copy, file.getName()).getSheets().stream()
                    .filter(sheet -> sheet.getName().equals(sheetName))
                    .findFirst()
                    .orElseThrow(() -> new IOException("The stored file has no sheet " + sheetName));
            return new Stored(rows, read);
        } catch (WorkbookReadException e) {
            throw new IOException(e);
        } finally {
            Files.deleteIfExists(copy.toPath());
        }
    }

    private static SheetStructure sheetOf(SessionFile file, String sheetName) {
        return file.getStructure().getSheets().stream()
                .filter(sheet -> sheet.getName().equals(sheetName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No sheet " + sheetName + " in " + file.getName()));
    }
}
