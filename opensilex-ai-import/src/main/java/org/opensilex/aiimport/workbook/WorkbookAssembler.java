//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.workbook;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Brings the files of a conversation together as one set of sheets.
 * <p>
 * Everything downstream — the description of each sheet, the merge of the objects, the reading of
 * the measures — sees sheets and nothing else, so a table is read the same whichever file it came
 * from, and an object named in a file and described in another is one object.
 *
 * @author Arnaud Charleroy
 */
public final class WorkbookAssembler {

    /**
     * Separates the file from the sheet in the name of a sheet whose name another file already uses.
     */
    public static final String QUALIFIER = " · ";

    private WorkbookAssembler() {
    }

    /**
     * @param files what was read from each file, in the order the user added them
     * @return one workbook whose sheets are those of every file in turn. The first to hold a name
     * keeps it; a later sheet of the same name is called {@code file · sheet}.
     */
    public static WorkbookStructure assemble(List<WorkbookStructure> files) {
        if (files.size() == 1) {
            return files.get(0);
        }
        WorkbookStructure assembled = new WorkbookStructure()
                .setFileName(files.stream().map(WorkbookStructure::getFileName).collect(Collectors.joining(", ")))
                .setFileSizeBytes(files.stream().mapToLong(WorkbookStructure::getFileSizeBytes).sum())
                .setDate1904(!files.isEmpty() && files.get(0).isDate1904());
        List<SheetStructure> sheets = new ArrayList<>();
        List<List<String>> names = sheetNames(files);
        for (int i = 0; i < files.size(); i++) {
            List<SheetStructure> own = files.get(i).getSheets();
            for (int j = 0; j < own.size(); j++) {
                sheets.add(own.get(j).renamed(names.get(i).get(j)).setIndex(sheets.size()));
            }
        }
        return assembled.setSheets(sheets);
    }

    /**
     * @return for each file, the name each of its sheets has in the assembled workbook
     */
    public static List<List<String>> sheetNames(List<WorkbookStructure> files) {
        Set<String> taken = new HashSet<>();
        List<List<String>> names = new ArrayList<>();
        for (WorkbookStructure file : files) {
            List<String> own = new ArrayList<>();
            for (SheetStructure sheet : file.getSheets()) {
                String name = taken.contains(key(sheet.getName()))
                        ? file.getFileName() + QUALIFIER + sheet.getName()
                        : sheet.getName();
                taken.add(key(name));
                own.add(name);
            }
            names.add(own);
        }
        return names;
    }

    private static String key(String sheetName) {
        return sheetName.toLowerCase(Locale.ROOT);
    }
}
