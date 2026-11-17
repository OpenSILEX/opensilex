//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api;

import org.apache.commons.io.FilenameUtils;
import org.opensilex.aiimport.AiImportConfig;
import org.opensilex.aiimport.workbook.SheetSources;

import javax.ws.rs.core.Response;
import java.io.File;
import java.util.Locale;
import java.util.Optional;

/**
 * What a file must be before it is opened, for every endpoint that accepts one: the same answer
 * whether it opens a conversation or is added to one.
 *
 * @author Arnaud Charleroy
 */
final class UploadRules {

    private UploadRules() {
    }

    /**
     * Rejects a file before it is opened, so that a wrong upload gives a clear answer instead of a
     * parser error, and so that an oversized one is never read into memory.
     *
     * @return the answer to send, or empty when the file is acceptable
     */
    static Optional<Response> reject(SessionAccess access, AiImportConfig config, File file, String fileName) {
        String extension = FilenameUtils.getExtension(fileName).toLowerCase(Locale.ROOT);
        if (!SheetSources.extensions().contains(extension)) {
            return Optional.of(access.badRequest("Only spreadsheets and CSV files are accepted.",
                    "The file " + fileName + " has the extension '" + extension + "'. Accepted "
                            + "extensions are " + String.join(", ", SheetSources.extensions()) + "."));
        }

        long maxBytes = (long) config.maxFileSizeMb() * 1024L * 1024L;
        if (file.length() > maxBytes) {
            return Optional.of(access.badRequest("The file is too large.",
                    "The file is " + (file.length() / (1024 * 1024)) + " MB, and the limit is "
                            + config.maxFileSizeMb() + " MB."));
        }
        return Optional.empty();
    }
}
