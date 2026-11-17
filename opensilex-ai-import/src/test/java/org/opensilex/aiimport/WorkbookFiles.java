//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.opensilex.aiimport.workbook.SheetStructure;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;

/**
 * Writes the sheets a test builds by hand into a real {@code .xlsx} file, so that an endpoint can
 * be given a file to upload without a binary being added to the repository.
 *
 * @author Arnaud Charleroy
 */
public final class WorkbookFiles {

    private WorkbookFiles() {
    }

    /**
     * @return a temporary file the caller deletes
     */
    public static File write(String prefix, SheetStructure... sheets) throws IOException {
        File file = File.createTempFile(prefix, ".xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(file.toPath())) {
            for (SheetStructure data : sheets) {
                Sheet sheet = workbook.createSheet(data.getName());
                fill(sheet.createRow(0), data.getHeaders());
                for (int i = 0; i < data.getRows().size(); i++) {
                    fill(sheet.createRow(i + 1), data.getRows().get(i));
                }
            }
            workbook.write(out);
        }
        return file;
    }

    private static void fill(Row row, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            row.createCell(i).setCellValue(cells.get(i));
        }
    }
}
