//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.export;

import java.io.IOException;

/**
 * Writes what an experiment holds as a workbook. The STAR export and the export by template are two
 * of them; the experiment is read once, whichever is asked for.
 *
 * @author Arnaud Charleroy
 */
public interface WorkbookExport {

    /**
     * @return the workbook, as the bytes of an {@code .xlsx} file
     */
    byte[] build(ExperimentSnapshot snapshot) throws IOException;
}
