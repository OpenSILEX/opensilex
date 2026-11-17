//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

import org.opensilex.aiimport.profile.ObjectRow;
import org.opensilex.aiimport.profile.ObjectTargets;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.workbook.HeaderMatcher;
import org.opensilex.aiimport.workbook.SheetStructure;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The objects of a workbook, one per name, whatever number of sheets describe them.
 * <p>
 * An object is its name across the whole workbook: a plan sheet that gives its block and treatment
 * and a sheet of plant material that gives its variety describe one object, created once, with
 * both. Each piece of information takes the first value that is not empty — the sheet the object
 * is attached to first, then the others in the order of the workbook — and two different values
 * for the same piece are a contradiction, which refuses the object: choosing one would be guessing.
 *
 * @author Arnaud Charleroy
 */
public final class ObjectMerger {

    static final String CONFLICT = DescriptionRules.PREFIX + "conflictingObject";
    static final String MORE_CONFLICTS = DescriptionRules.PREFIX + "moreConflictingObjects";

    /**
     * Separates the sheet from the column in the header of a column that comes from another sheet
     * than the one the object is attached to.
     */
    public static final String QUALIFIER = " · ";

    private static final int MAX_REPORTED_CONFLICTS = 10;

    /**
     * What a column can say about an object, as opposed to about an observation.
     */
    private static final Set<DescribedRole> DESCRIBES_THE_OBJECT = EnumSet.of(DescribedRole.GERMPLASM,
            DescribedRole.FACTOR_LEVEL, DescribedRole.PARENT_OBJECT, DescribedRole.POSITION,
            DescribedRole.OBJECT_PROPERTY, DescribedRole.LOCATION, DescribedRole.COMMENT);

    /**
     * @param rows            the objects, in the order they first appear
     * @param targetsBySheet  for the sheet each object is attached to, what each of its columns
     *                        becomes — including the columns brought from other sheets, under their
     *                        qualified header
     * @param anomalies       the contradictions, as many as are worth showing
     */
    public record Merged(List<ObjectRow> rows, Map<String, Map<String, String>> targetsBySheet,
                         List<ReportMessage> anomalies) {
    }

    private record Info(String value, String sheet, int row, String column) {
    }

    private static final class Accumulator {
        private final String name;
        private final DescribedSheet attach;
        private final int attachRow;
        private final Map<String, Info> info = new LinkedHashMap<>();
        private final Map<String, String> cells = new LinkedHashMap<>();
        private final List<ReportMessage> problems = new ArrayList<>();
        private final Set<String> conflicting = new HashSet<>();

        private Accumulator(String name, DescribedSheet attach, int attachRow) {
            this.name = name;
            this.attach = attach;
            this.attachRow = attachRow;
        }

        private String value(String key) {
            Info found = info.get(key);
            return found == null ? null : found.value();
        }
    }

    private ObjectMerger() {
    }

    /**
     * @param sheets every sheet of the workbook, in its order; only those read and describing
     *               objects take part
     */
    public static Merged merge(List<DescribedSheet> sheets) {
        List<DescribedSheet> contributors = new ArrayList<>();
        for (SheetKind kind : new SheetKind[]{SheetKind.OBJECTS, SheetKind.OBJECTS_AND_DATA}) {
            for (DescribedSheet sheet : sheets) {
                if (sheet.isReadable() && sheet.kind() == kind && sheet.column(DescribedRole.OBJECT) != null) {
                    contributors.add(sheet);
                }
            }
        }

        Map<String, Accumulator> byName = new LinkedHashMap<>();
        Map<String, Map<String, String>> targets = new LinkedHashMap<>();
        List<ReportMessage> conflicts = new ArrayList<>();
        for (DescribedSheet sheet : contributors) {
            SheetStructure data = sheet.sheet();
            String nameColumn = sheet.column(DescribedRole.OBJECT);
            for (int i = 0; i < data.getRows().size(); i++) {
                List<String> row = data.getRows().get(i);
                String name = data.cell(row, nameColumn).trim();
                if (name.isEmpty()) {
                    continue;
                }
                // The first sheet to name an object is the one it is attached to, since the
                // contributors are ordered with the sheets that only describe objects first.
                int rowNumber = i + 2;
                Accumulator object = byName.computeIfAbsent(name,
                        key -> new Accumulator(key, sheet, rowNumber));
                absorb(object, sheet, row, rowNumber, targets, conflicts);
            }
        }

        List<ObjectRow> rows = new ArrayList<>();
        for (Accumulator object : byName.values()) {
            rows.add(toRow(object));
        }
        return new Merged(rows, targets, capped(conflicts));
    }

    private static void absorb(Accumulator object, DescribedSheet sheet, List<String> row, int rowNumber,
                               Map<String, Map<String, String>> targets, List<ReportMessage> conflicts) {
        SheetStructure data = sheet.sheet();
        String nameColumn = sheet.column(DescribedRole.OBJECT);
        boolean own = sheet.name().equals(object.attach.name());
        Map<String, String> attachTargets = targets.computeIfAbsent(object.attach.name(),
                key -> new LinkedHashMap<>());
        attachTargets.putIfAbsent(object.attach.column(DescribedRole.OBJECT), ObjectTargets.NAME);
        for (String header : data.getHeaders()) {
            if (header == null || header.isEmpty() || header.equals(nameColumn)) {
                continue;
            }
            DescribedRole role = sheet.roleOf(header);
            boolean describes = DESCRIBES_THE_OBJECT.contains(role);
            if (!own && !describes) {
                continue;
            }
            String value = data.cell(row, header).trim();
            String cellKey = own ? header : sheet.name() + QUALIFIER + header;
            object.cells.merge(cellKey, value, (old, now) -> old.isEmpty() ? now : old);
            attachTargets.putIfAbsent(cellKey, targetOf(sheet, header, role));
            if (describes && !value.isEmpty()) {
                record(object, infoKey(role, header), value, sheet.name(), rowNumber, header, conflicts);
            }
        }
    }

    /**
     * Keeps the first value of each piece of information, and says so when a later one differs.
     */
    private static void record(Accumulator object, String key, String value, String sheet, int row,
                               String column, List<ReportMessage> conflicts) {
        Info existing = object.info.get(key);
        if (existing == null) {
            object.info.put(key, new Info(value, sheet, row, column));
            return;
        }
        if (existing.value().equalsIgnoreCase(value) || !object.conflicting.add(key)) {
            return;
        }
        ReportMessage problem = ReportMessage.of(CONFLICT, "Object '" + object.name + "' is described with two "
                        + "different values: '" + existing.value() + "' (sheet '" + existing.sheet()
                        + "', row " + existing.row() + ", column '" + existing.column() + "') and '" + value
                        + "' (sheet '" + sheet + "', row " + row + ", column '" + column + "').")
                .with("object", object.name)
                .with("value", existing.value())
                .with("sheet", existing.sheet())
                .with("row", existing.row())
                .with("column", existing.column())
                .with("otherValue", value)
                .with("otherSheet", sheet)
                .with("otherRow", row)
                .with("otherColumn", column);
        object.problems.add(problem);
        conflicts.add(problem);
    }

    /**
     * What a column says about an object, so that the same thing said by two sheets is compared.
     * The level columns are told apart by their header, which names the factor.
     */
    private static String infoKey(DescribedRole role, String header) {
        switch (role) {
            case GERMPLASM:
                return "germplasm";
            case FACTOR_LEVEL:
                return "factor:" + HeaderMatcher.normalize(header);
            case PARENT_OBJECT:
                return "parent";
            case LOCATION:
                return "facility";
            case POSITION:
                return axis(header);
            default:
                return "column:" + header;
        }
    }

    /**
     * What a column becomes in the object sheets panel. The role settles it, but for a position,
     * which depends on the axis the header names; a property, which is what the user chose; and a
     * comment, which only a sheet of objects offers as one.
     */
    private static String targetOf(DescribedSheet sheet, String header, DescribedRole role) {
        switch (role) {
            case POSITION:
                return "y".equals(axis(header)) ? ObjectTargets.Y : ObjectTargets.X;
            case OBJECT_PROPERTY:
                return sheet.description() == null ? ObjectTargets.NONE
                        : sheet.description().getProperties().getOrDefault(header, ObjectTargets.NONE);
            case COMMENT:
                return sheet.kind() == SheetKind.OBJECTS ? ObjectTargets.COMMENT : ObjectTargets.NONE;
            default:
                return role.defaultObjectTarget();
        }
    }

    /**
     * @return {@code "y"} when the header names the second axis of a position, {@code "x"} otherwise
     */
    public static String axis(String header) {
        return header.trim().toLowerCase(Locale.ROOT).endsWith("y") ? "y" : "x";
    }

    private static ObjectRow toRow(Accumulator object) {
        ObjectRow row = new ObjectRow(object.attach.name(), object.attachRow, object.name)
                .setGermplasm(object.value("germplasm"))
                .setFacility(object.value("facility"))
                .setPosition(object.value("x"), object.value("y"));
        for (Map.Entry<String, Info> entry : object.info.entrySet()) {
            if (entry.getKey().startsWith("factor:")) {
                row.setFactorLevel(entry.getValue().value());
                break;
            }
        }
        row.getCells().putAll(object.cells);
        object.problems.forEach(row::addProblem);
        return row;
    }

    private static List<ReportMessage> capped(List<ReportMessage> conflicts) {
        if (conflicts.size() <= MAX_REPORTED_CONFLICTS) {
            return conflicts;
        }
        List<ReportMessage> shown = new ArrayList<>(conflicts.subList(0, MAX_REPORTED_CONFLICTS));
        int more = conflicts.size() - MAX_REPORTED_CONFLICTS;
        shown.add(ReportMessage.of(MORE_CONFLICTS, more + " more objects are described with two different "
                + "values.").with("count", more));
        return shown;
    }
}
