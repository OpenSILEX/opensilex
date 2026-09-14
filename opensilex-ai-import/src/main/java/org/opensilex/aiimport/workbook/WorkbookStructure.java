//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.workbook;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What was read from an uploaded workbook. Holds only headers, a bounded row sample and the prose
 * of non-tabular sheets, never the whole file: this is the object serialised into the language
 * model prompt.
 *
 * @author Arnaud Charleroy
 */
public class WorkbookStructure {

    private String fileName;
    private long fileSizeBytes;
    private List<SheetStructure> sheets = new ArrayList<>();

    /**
     * True when the workbook uses the 1904 date system rather than 1900. Worth knowing because the
     * same serial reads four years apart in the two systems.
     */
    private boolean date1904;

    public String getFileName() {
        return fileName;
    }

    public WorkbookStructure setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public WorkbookStructure setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
        return this;
    }

    public List<SheetStructure> getSheets() {
        return sheets;
    }

    public WorkbookStructure setSheets(List<SheetStructure> sheets) {
        this.sheets = sheets;
        return this;
    }

    public boolean isDate1904() {
        return date1904;
    }

    public WorkbookStructure setDate1904(boolean date1904) {
        this.date1904 = date1904;
        return this;
    }

    public Optional<SheetStructure> getSheet(String name) {
        return sheets.stream()
                .filter(sheet -> sheet.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public boolean hasSheet(String name) {
        return getSheet(name).isPresent();
    }

    public List<String> getSheetNames() {
        List<String> names = new ArrayList<>(sheets.size());
        sheets.forEach(sheet -> names.add(sheet.getName()));
        return names;
    }
}
