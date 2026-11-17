//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service;

import org.opensilex.aiimport.workbook.WorkbookAssembler;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * What adding a file to a conversation, or taking one away, does to it. Nothing here reads a file
 * or touches the instance: the caller reads and stores, and works the report out afterwards.
 *
 * @author Arnaud Charleroy
 */
public final class SessionFiles {

    private SessionFiles() {
    }

    /**
     * @return what stands in the way of adding a file of this name, or empty when it can be added
     */
    public static Optional<String> whyNotAdd(AiImportSession session, String fileName) {
        if (!SessionProfile.isDescribable(session)) {
            return Optional.of("This conversation is read by a template: its file is known to the module, "
                    + "and files cannot be added to it.");
        }
        if (session.getFiles().size() >= AiImportSession.MAX_FILES) {
            return Optional.of("A conversation holds at most " + AiImportSession.MAX_FILES + " files.");
        }
        if (session.getFiles().stream().anyMatch(file -> file.getName().equalsIgnoreCase(fileName))) {
            return Optional.of("A file called '" + fileName + "' is already in this conversation.");
        }
        return Optional.empty();
    }

    /**
     * @return what stands in the way of removing this file, or empty when it can be removed
     */
    public static Optional<String> whyNotRemove(AiImportSession session) {
        if (!SessionProfile.isDescribable(session)) {
            return Optional.of("This conversation is read by a template: its file is known to the module.");
        }
        if (session.getFiles().size() <= 1) {
            return Optional.of("A conversation keeps at least one file. Delete the conversation instead.");
        }
        return Optional.empty();
    }

    /**
     * Adds a file read by the caller, and describes the conversation from then on as the user does:
     * the sheets of the new file have no template to be read by.
     */
    public static SessionFile add(AiImportSession session, String fileName, WorkbookStructure structure) {
        SessionFile file = SessionFile.of(fileName, structure);
        session.getFiles().add(file);
        session.assemble();
        return file;
    }

    /**
     * Takes a file out, with what the user said of its sheets.
     *
     * @return the file removed, or empty when the conversation holds none with this identifier
     */
    public static Optional<SessionFile> remove(AiImportSession session, String fileId) {
        List<SessionFile> files = session.getFiles();
        Optional<SessionFile> found = files.stream().filter(file -> file.getId().equals(fileId)).findFirst();
        if (found.isEmpty()) {
            return found;
        }
        int index = files.indexOf(found.get());
        List<String> sheets = WorkbookAssembler
                .sheetNames(files.stream().map(SessionFile::getStructure).collect(Collectors.toList()))
                .get(index);
        files.remove(index);
        session.assemble().forgetSheets(sheets);
        session.setFileName(files.get(0).getName());
        return found;
    }
}
