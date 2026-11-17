//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.workbook;

import org.opensilex.aiimport.exception.WorkbookReadException;

import java.io.File;
import java.util.List;

/**
 * Reads one kind of file into sheets. A new format is a new implementation listed in
 * {@link SheetSources}; nothing else knows which formats exist.
 *
 * @author Arnaud Charleroy
 */
public interface SheetSource {

    /**
     * @return the file extensions this source reads, lower case, without the dot
     */
    List<String> extensions();

    /**
     * @param originalFileName the name the user gave the file, which names the sheet when the
     *                         format has none of its own
     */
    WorkbookStructure read(File file, String originalFileName) throws WorkbookReadException;
}
