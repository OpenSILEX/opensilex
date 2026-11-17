//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.workbook;

import org.apache.commons.io.FilenameUtils;
import org.opensilex.aiimport.exception.WorkbookReadException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The formats the module reads, and the one that reads a given file. The single list the upload
 * checks, the readers and the interface's file picker all come from.
 *
 * @author Arnaud Charleroy
 */
public final class SheetSources {

    private static final List<SheetSource> SOURCES = List.of(new WorkbookReader(), new CsvReader());

    private SheetSources() {
    }

    /**
     * @return every extension a file may have, lower case, without the dot
     */
    public static List<String> extensions() {
        List<String> extensions = new ArrayList<>();
        SOURCES.forEach(source -> extensions.addAll(source.extensions()));
        return extensions;
    }

    public static Optional<SheetSource> forFile(String fileName) {
        String extension = FilenameUtils.getExtension(fileName).toLowerCase(Locale.ROOT);
        return SOURCES.stream().filter(source -> source.extensions().contains(extension)).findFirst();
    }

    /**
     * Reads a file with the source its extension names.
     *
     * @throws WorkbookReadException when no source reads this kind of file, or the file is unreadable
     */
    public static WorkbookStructure read(File file, String originalFileName) throws WorkbookReadException {
        SheetSource source = forFile(originalFileName).orElseThrow(() -> new WorkbookReadException(
                originalFileName, new IllegalArgumentException("no reader for this kind of file")));
        return source.read(file, originalFileName);
    }
}
