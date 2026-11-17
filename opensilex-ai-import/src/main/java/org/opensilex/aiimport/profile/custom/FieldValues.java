//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

import org.opensilex.aiimport.workbook.ExcelValueParser;
import org.opensilex.aiimport.workbook.SheetStructure;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What the columns of a file say for the fields of the forms that create something: the text of
 * several columns put in one field, and what the rows of an experiment or a project name add up to.
 * <p>
 * Several columns in one text field are joined with {@value #SEPARATOR}, each value carrying the
 * name of its column so that the person reading the field sees where it came from. A single column
 * stands as it is: a label in front of the only thing said would add nothing.
 *
 * @author Arnaud Charleroy
 */
public final class FieldValues {

    public static final String SEPARATOR = " ; ";

    private FieldValues() {
    }

    /**
     * @param columns the columns that feed the field, in the order of the sheet
     * @return what the row says in them, or {@code null} when every cell is empty
     */
    public static String joined(SheetStructure data, List<String> row, List<String> columns) {
        List<String> parts = new ArrayList<>();
        for (String column : columns) {
            String value = data.cell(row, column).trim();
            if (!value.isEmpty()) {
                parts.add(columns.size() > 1 ? column + ": " + value : value);
            }
        }
        return parts.isEmpty() ? null : String.join(SEPARATOR, parts);
    }

    /**
     * What the rows of the sheets of a kind say, by the name their name column gives: the columns
     * described as comments, put together, for the description of a facility or of an event.
     *
     * @param nameRole the role of the column that names what the row is
     */
    public static Map<String, String> commentsOf(List<DescribedSheet> sheets, SheetKind kind, DescribedRole nameRole) {
        Map<String, Set<String>> byName = new LinkedHashMap<>();
        for (DescribedSheet sheet : sheets) {
            String nameColumn = sheet.column(nameRole);
            if (!sheet.isReadable() || sheet.kind() != kind || nameColumn == null) {
                continue;
            }
            List<String> comments = sheet.columns(DescribedRole.COMMENT);
            for (List<String> row : sheet.sheet().getRows()) {
                String name = sheet.sheet().cell(row, nameColumn).trim();
                String text = joined(sheet.sheet(), row, comments);
                if (!name.isEmpty() && text != null) {
                    byName.computeIfAbsent(name, key -> new LinkedHashSet<>()).add(text);
                }
            }
        }
        Map<String, String> result = new LinkedHashMap<>();
        byName.forEach((name, texts) -> result.put(name, String.join(SEPARATOR, texts)));
        return result;
    }

    /**
     * What the columns described as fields of a form say of each experiment or project the sheets
     * name, by the name: for the form that creates it. A text field adds up what each row says; any
     * other field keeps the first value that is not empty, and a date that is none is left out.
     */
    public static Map<String, Map<String, String>> detailsOf(List<DescribedSheet> sheets, FormTarget.Entity entity) {
        Map<String, Map<String, Set<String>>> collected = new LinkedHashMap<>();
        for (DescribedSheet sheet : sheets) {
            String nameColumn = sheet.column(entity.nameRole());
            if (!sheet.isReadable() || nameColumn == null) {
                continue;
            }
            for (DescribedRole role : DescribedRole.values()) {
                FormTarget target = role.formTarget().orElse(null);
                if (target == null || target.entity() != entity || sheet.columns(role).isEmpty()) {
                    continue;
                }
                for (List<String> row : sheet.sheet().getRows()) {
                    String name = sheet.sheet().cell(row, nameColumn).trim();
                    String value = valueOf(sheet, row, sheet.columns(role), target);
                    if (!name.isEmpty() && value != null) {
                        collected.computeIfAbsent(name, key -> new LinkedHashMap<>())
                                .computeIfAbsent(target.field(), key -> new LinkedHashSet<>()).add(value);
                    }
                }
            }
        }
        if (entity == FormTarget.Entity.EXPERIMENT) {
            linkedNames(sheets, collected);
        }
        Map<String, Map<String, String>> details = new LinkedHashMap<>();
        collected.forEach((name, fields) -> {
            Map<String, String> byField = new LinkedHashMap<>();
            fields.forEach((field, values) -> byField.put(field, isConcatenated(entity, field)
                    ? String.join(SEPARATOR, values) : values.iterator().next()));
            details.put(name, byField);
        });
        return details;
    }

    /**
     * The field of the experiment form that lists the projects, and the one that lists the facilities:
     * what a sheet of experiments names on the row of each.
     */
    public static final String PROJECTS = "projects";
    public static final String FACILITIES = "facilities";

    /**
     * The projects and facilities a sheet of experiments names on the row of an experiment, by name:
     * the form wants their URIs, which the report gives once they are found.
     */
    private static void linkedNames(List<DescribedSheet> sheets, Map<String, Map<String, Set<String>>> collected) {
        for (DescribedSheet sheet : sheets) {
            String nameColumn = sheet.column(DescribedRole.TRIAL);
            if (!sheet.isReadable() || sheet.kind() != SheetKind.EXPERIMENTS || nameColumn == null) {
                continue;
            }
            for (List<String> row : sheet.sheet().getRows()) {
                String name = sheet.sheet().cell(row, nameColumn).trim();
                if (name.isEmpty()) {
                    continue;
                }
                link(collected, name, PROJECTS, sheet, row, sheet.columns(DescribedRole.PROJECT));
                link(collected, name, FACILITIES, sheet, row, sheet.columns(DescribedRole.LOCATION));
            }
        }
    }

    private static void link(Map<String, Map<String, Set<String>>> collected, String experiment, String field,
                             DescribedSheet sheet, List<String> row, List<String> columns) {
        for (String column : columns) {
            String value = sheet.sheet().cell(row, column).trim();
            if (!value.isEmpty()) {
                collected.computeIfAbsent(experiment, key -> new LinkedHashMap<>())
                        .computeIfAbsent(field, key -> new LinkedHashSet<>()).add(value);
            }
        }
    }

    private static boolean isConcatenated(FormTarget.Entity entity, String field) {
        if (PROJECTS.equals(field) || FACILITIES.equals(field)) {
            return true;
        }
        for (FormTarget target : FormTarget.values()) {
            if (target.entity() == entity && target.field().equals(field)) {
                return target.isConcatenated();
            }
        }
        return false;
    }

    private static String valueOf(DescribedSheet sheet, List<String> row, List<String> columns, FormTarget target) {
        if (target.isConcatenated()) {
            return joined(sheet.sheet(), row, columns);
        }
        for (String column : columns) {
            String value = sheet.sheet().cell(row, column).trim();
            if (value.isEmpty()) {
                continue;
            }
            if (!target.isDate()) {
                return value;
            }
            LocalDate date = ExcelValueParser.parseDate(value, sheet.sheet().isDate1904());
            if (date != null) {
                return date.toString();
            }
        }
        return null;
    }
}
