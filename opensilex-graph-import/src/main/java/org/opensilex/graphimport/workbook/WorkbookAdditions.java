//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.workbook;

import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;
import org.opensilex.graphimport.binding.ImportDecisions;
import org.opensilex.graphimport.binding.ImportDecisions.DeclaredResource;
import org.opensilex.graphimport.binding.ImportDecisions.SheetDecisions;
import org.opensilex.graphimport.interpret.ClassCatalog;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The workbook the assistant reads: the files' sheets, with what the user added in the assistant.
 * <p>
 * A value given for every row of a sheet becomes a column added at the end of the sheet, its header the
 * URI of the property and each cell the value. The resources the user declared become the rows of a sheet
 * added for their class, the headers the URIs of their properties, the first column their name. A header
 * written as a property is read without asking, so everything downstream (interpretation, merge, links,
 * check, preview) treats them as it treats the files, and each value keeps a cell to point at.
 * <p>
 * The files' sheets are never changed: a sheet with columns added is a copy.
 *
 * @author Arnaud Charleroy
 */
public final class WorkbookAdditions {

    private WorkbookAdditions() {
    }

    /**
     * The workbook read, the decisions it is read with, and what was added to it.
     *
     * @param workbook     the files' sheets with what was added; the files' workbook itself when nothing was
     * @param decisions    the user's decisions, with the class and identifier column of each added sheet
     * @param fileColumns  how many columns each sheet of the files has; a column past them was added
     * @param declaredRows for each added sheet, the identifier of the resource on each row, in row order
     */
    public record ReadWorkbook(WorkbookStructure workbook, ImportDecisions decisions, Map<String, Integer> fileColumns,
                               Map<String, List<String>> declaredRows) {

        public ReadWorkbook {
            fileColumns = Map.copyOf(fileColumns);
            declaredRows = Map.copyOf(declaredRows);
        }

        /** Whether the column was added for a value the user gave, rather than read from a file. */
        public boolean isGiven(String sheet, int column) {
            Integer columns = fileColumns.get(sheet);
            return columns != null && column >= columns;
        }

        /** Whether the sheet was added for resources the user declared. */
        public boolean isDeclared(String sheet) {
            return declaredRows.containsKey(sheet);
        }

        /** The identifier of the resource on each row of an added sheet; empty for a sheet of the files. */
        public List<String> declaredRowsOf(String sheet) {
            return declaredRows.getOrDefault(sheet, List.of());
        }
    }

    public static ReadWorkbook apply(WorkbookStructure files, ImportDecisions decisions) {
        Map<String, Integer> fileColumns = new HashMap<>();
        files.getSheets().forEach(sheet -> fileColumns.put(sheet.getName(), sheet.getHeaders().size()));
        boolean anyGiven = decisions.sheets().values().stream().anyMatch(decided -> !decided.given().isEmpty());
        if (!anyGiven && decisions.declared().isEmpty()) {
            return new ReadWorkbook(files, decisions, fileColumns, Map.of());
        }

        List<SheetStructure> sheets = new ArrayList<>();
        for (SheetStructure sheet : files.getSheets()) {
            SheetDecisions decided = decisions.of(sheet.getName());
            sheets.add(decided.given().isEmpty() || !sheet.isTabular() ? sheet : withGiven(sheet, decided.given()));
        }

        Map<String, SheetDecisions> allDecisions = new LinkedHashMap<>(decisions.sheets());
        Map<String, List<String>> declaredRows = new LinkedHashMap<>();
        Set<String> names = new HashSet<>(files.getSheetNames());
        byClass(decisions.declared()).forEach((classUri, resources) -> {
            String name = uniqueName("[" + ClassCatalog.localName(classUri) + "]", names);
            names.add(name);
            sheets.add(declaredSheet(name, files.getSheets().size() + declaredRows.size(), resources));
            allDecisions.put(name, new SheetDecisions(classUri, 0, Map.of()));
            declaredRows.put(name, resources.stream().map(DeclaredResource::id).toList());
        });

        WorkbookStructure read = new WorkbookStructure()
                .setFileName(files.getFileName())
                .setFileSizeBytes(files.getFileSizeBytes())
                .setDate1904(files.isDate1904())
                .setSheets(sheets);
        return new ReadWorkbook(read, new ImportDecisions(allDecisions, decisions.declared()), fileColumns,
                declaredRows);
    }

    /** A copy of the sheet with one column per given value, every row holding it. */
    private static SheetStructure withGiven(SheetStructure sheet, Map<URI, String> given) {
        int width = sheet.getHeaders().size();
        List<String> headers = new ArrayList<>(sheet.getHeaders());
        given.keySet().forEach(property -> headers.add(property.toString()));
        List<List<String>> rows = new ArrayList<>();
        for (List<String> row : sheet.getRows()) {
            List<String> extended = new ArrayList<>(row.subList(0, Math.min(row.size(), width)));
            while (extended.size() < width) {
                extended.add("");
            }
            extended.addAll(given.values());
            rows.add(extended);
        }
        return copyOf(sheet).setHeaders(headers).setRows(rows);
    }

    /** The resources of each class, classes and resources in the order they were declared. */
    private static Map<URI, List<DeclaredResource>> byClass(List<DeclaredResource> declared) {
        Map<URI, List<DeclaredResource>> byClass = new LinkedHashMap<>();
        declared.forEach(resource -> byClass.computeIfAbsent(resource.classUri(), key -> new ArrayList<>())
                .add(resource));
        return byClass;
    }

    /**
     * The sheet of the resources of one class: their name first, then each other property in the order it
     * first appears, with as many columns as the resource that has the most values for it.
     */
    private static SheetStructure declaredSheet(String name, int index, List<DeclaredResource> resources) {
        Map<URI, Integer> widths = new LinkedHashMap<>();
        widths.put(ImportDecisions.LABEL, 1);
        resources.forEach(resource -> resource.values().forEach((property, values) ->
                widths.merge(property, values.size(), Math::max)));
        List<String> headers = new ArrayList<>();
        widths.forEach((property, width) -> {
            for (int column = 0; column < width; column++) {
                headers.add(property.toString());
            }
        });
        List<List<String>> rows = new ArrayList<>();
        for (DeclaredResource resource : resources) {
            List<String> row = new ArrayList<>();
            widths.forEach((property, width) -> {
                List<String> values = resource.values().getOrDefault(property, List.of());
                for (int column = 0; column < width; column++) {
                    row.add(column < values.size() ? values.get(column) : "");
                }
            });
            rows.add(row);
        }
        return new SheetStructure().setName(name).setIndex(index).setHeaders(headers).setRows(rows).setTabular(true);
    }

    private static String uniqueName(String wanted, Set<String> taken) {
        String name = wanted;
        for (int suffix = 2; taken.contains(name); suffix++) {
            name = wanted + " " + suffix;
        }
        return name;
    }

    private static SheetStructure copyOf(SheetStructure sheet) {
        return new SheetStructure().setName(sheet.getName()).setIndex(sheet.getIndex()).setTabular(sheet.isTabular())
                .setText(sheet.getText()).setDate1904(sheet.isDate1904()).setCharset(sheet.getCharset());
    }
}
