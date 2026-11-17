//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.session;

import org.opensilex.aiimport.exception.WorkbookReadException;
import org.opensilex.aiimport.service.SessionFile;
import org.opensilex.aiimport.workbook.SheetSources;
import org.opensilex.graphimport.session.store.GraphImportSnapshot;
import org.opensilex.graphimport.session.store.SnapshotSessionStore;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Reads the files of an import, when they arrive and when the import is resumed from the store.
 * <p>
 * Both go through the platform's workbook readers, so a file reads the same on the day it is
 * uploaded and on the day the user comes back to it.
 *
 * @author Arnaud Charleroy
 */
public final class SessionFiles {

    private SessionFiles() {
    }

    /**
     * @return the file, read
     * @throws WorkbookReadException when it is not a readable workbook or CSV file
     */
    public static SessionFile read(File file, String fileName) throws WorkbookReadException {
        return SessionFile.of(fileName, SheetSources.read(file, fileName));
    }

    /**
     * Rebuilds a stored import: its state from the store, its sheets from its stored files.
     *
     * @return the import, or empty when the owner has no such import
     */
    public static Optional<GraphImportSession> resume(SnapshotSessionStore<GraphImportSnapshot> store,
                                                      String sessionId, URI owner)
            throws IOException, WorkbookReadException {
        Optional<GraphImportSnapshot> saved = store.load(sessionId, owner);
        if (saved.isEmpty()) {
            return Optional.empty();
        }
        Instant createdAt = saved.get().getCreatedAt() == null ? Instant.now() : saved.get().getCreatedAt();
        GraphImportSession session = new GraphImportSession(sessionId, owner, createdAt);
        for (GraphImportSnapshot.StoredFile stored : saved.get().getFiles()) {
            File copy = temporaryCopy(stored.getName(), store.readFile(sessionId, stored.getId()));
            try {
                SessionFile file = new SessionFile(stored.getId(), stored.getName(),
                        SheetSources.read(copy, stored.getName()));
                session.add(file);
                List<GraphImportSnapshot.StoredLayout> layouts = saved.get().getLayouts().stream()
                        .filter(layout -> stored.getId().equals(layout.getFileId()))
                        .toList();
                if (!layouts.isEmpty()) {
                    SheetLayouts.reapply(session, file, copy, layouts);
                }
            } finally {
                Files.deleteIfExists(copy.toPath());
            }
        }
        session.setExperiment(saved.get().getExperiment());
        session.setProvenance(saved.get().getProvenance());
        saved.get().getDecisions().forEach(stored -> session.decide(stored.getSheet(), stored.toModel()));
        saved.get().getDeclared().stream()
                .filter(stored -> stored.getId() != null && stored.getClassUri() != null)
                .forEach(stored -> session.declare(stored.toModel()));
        saved.get().getCreations().stream().filter(record -> record != null && record.step() != null)
                .forEach(session::recordCreation);
        return Optional.of(session);
    }

    /**
     * A temporary file of the kind the name says, which is what tells the reader how to read it. Only
     * the extension is taken from the name, and only when a reader exists for it.
     */
    static File temporaryCopy(String fileName, byte[] content) throws IOException {
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        String extension = SheetSources.forFile(fileName)
                .flatMap(source -> source.extensions().stream()
                        .filter(candidate -> lowerName.endsWith("." + candidate))
                        .findFirst())
                .orElseThrow(() -> new IOException("No reader for the stored file " + fileName));
        File copy = File.createTempFile("graph-import-", "." + extension);
        Files.write(copy.toPath(), content);
        return copy;
    }
}
