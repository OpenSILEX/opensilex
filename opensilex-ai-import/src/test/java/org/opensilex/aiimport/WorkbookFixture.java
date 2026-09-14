//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport;

import org.opensilex.aiimport.exception.WorkbookReadException;
import org.opensilex.aiimport.workbook.WorkbookReader;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.io.File;
import java.net.URL;

/**
 * Gives the tests the sample VitisExplorer workbook shipped in the test resources.
 *
 * @author Arnaud Charleroy
 */
public final class WorkbookFixture {

    public static final String VITIS_FILE_NAME = "SaisieVitisExplorer20.xlsx";

    /**
     * The STAR reference template: variables and metadata in two dictionary sheets, and the
     * experimental design sheets prefixed {@code ed_}.
     */
    public static final String STAR_STANDARD_FILE_NAME = "STAR_standard.xlsx";

    /**
     * A filled STAR workbook of the earlier revision: one merged dictionary sheet, and the design
     * sheets without their prefix.
     */
    public static final String STAR_EXAMPLE_FILE_NAME = "STAR_exemple.xlsx";

    /**
     * The MIAPPE v1.1 training spreadsheet: every section of the checklist, documented, with the
     * value rows left empty — which is how a submission starts out.
     */
    public static final String MIAPPE_FILE_NAME = "MIAPPE_training.xlsx";

    private static WorkbookStructure vitis;
    private static WorkbookStructure starStandard;
    private static WorkbookStructure starExample;
    private static WorkbookStructure miappe;

    private WorkbookFixture() {
    }

    public static File vitisFile() {
        return file(VITIS_FILE_NAME);
    }

    public static File file(String name) {
        URL resource = WorkbookFixture.class.getClassLoader().getResource(name);
        if (resource == null) {
            throw new IllegalStateException(name + " is missing from the test resources");
        }
        return new File(resource.getFile());
    }

    /**
     * Read once and shared: opening the workbook is the slowest part of these tests.
     */
    public static synchronized WorkbookStructure vitis() throws WorkbookReadException {
        if (vitis == null) {
            vitis = new WorkbookReader().read(vitisFile(), VITIS_FILE_NAME);
        }
        return vitis;
    }

    public static synchronized WorkbookStructure starStandard() throws WorkbookReadException {
        if (starStandard == null) {
            starStandard = new WorkbookReader()
                    .read(file(STAR_STANDARD_FILE_NAME), STAR_STANDARD_FILE_NAME);
        }
        return starStandard;
    }

    public static synchronized WorkbookStructure miappe() throws WorkbookReadException {
        if (miappe == null) {
            miappe = new WorkbookReader().read(file(MIAPPE_FILE_NAME), MIAPPE_FILE_NAME);
        }
        return miappe;
    }

    public static synchronized WorkbookStructure starExample() throws WorkbookReadException {
        if (starExample == null) {
            starExample = new WorkbookReader()
                    .read(file(STAR_EXAMPLE_FILE_NAME), STAR_EXAMPLE_FILE_NAME);
        }
        return starExample;
    }
}
