//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.objects;

import org.opensilex.aiimport.create.objects.TypeProperties.TypeProperty;
import org.opensilex.aiimport.profile.ImportProfile;
import org.opensilex.aiimport.profile.ImportProfileRegistry;
import org.opensilex.aiimport.profile.ObjectRow;
import org.opensilex.aiimport.profile.ObjectSheetDefaults;
import org.opensilex.aiimport.profile.ObjectTargets;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The object sheets of a conversation, as the next creation would read them.
 * <p>
 * Built on demand from three sources, in that order of precedence: what the user chose for the
 * sheet, what the profile knows of its columns, and nothing — a column nobody mapped is read and
 * shown, not written. Never cached: a revalidation or a new choice changes the answer, and a view
 * computed before either would describe objects that will not be created.
 *
 * @author Arnaud Charleroy
 */
public final class ObjectSheets {

    public static final String PROBLEM = "AiImport.objects.problem.";

    private ObjectSheets() {
    }

    /**
     * @return the sheets listing objects, in the order of the workbook; nothing when the profile
     * cannot tell which sheet lists them
     */
    public static List<ObjectSheet> of(AiImportSession session) {
        if (session.getWorkbook() == null) {
            return Collections.emptyList();
        }
        Optional<ImportProfile> found = new ImportProfileRegistry().getById(session.getProfileId());
        if (!found.isPresent()) {
            return Collections.emptyList();
        }
        ImportProfile profile = found.get();

        Map<String, List<ObjectRow>> bySheet = new LinkedHashMap<>();
        for (ObjectRow row : profile.extractObjectRows(session.getWorkbook())) {
            bySheet.computeIfAbsent(row.getSheet(), sheet -> new ArrayList<>()).add(row);
        }

        List<ObjectSheet> sheets = new ArrayList<>();
        bySheet.forEach((name, rows) -> sheets.add(sheet(name, rows,
                profile.objectSheetDefaults(session.getWorkbook(), name),
                session.getObjectPlans().get(name))));
        return sheets;
    }

    public static Optional<ObjectSheet> named(AiImportSession session, String sheet) {
        return of(session).stream().filter(candidate -> candidate.getName().equals(sheet)).findFirst();
    }

    private static ObjectSheet sheet(String name, List<ObjectRow> rows, ObjectSheetDefaults defaults,
                                     ObjectSheetPlan plan) {
        Set<String> headers = new LinkedHashSet<>();
        rows.forEach(row -> headers.addAll(row.getCells().keySet()));

        Map<String, String> mapping = new LinkedHashMap<>();
        for (String header : headers) {
            String target = plan != null && plan.getMapping().containsKey(header)
                    ? plan.getMapping().get(header)
                    : defaults.targets().getOrDefault(header, ObjectTargets.NONE);
            mapping.put(header, target == null ? ObjectTargets.NONE : target);
        }
        // The name is the name: it identifies the object, in the file and in the report, and a
        // mapping that sent it elsewhere would create objects nobody can find again.
        if (defaults.nameColumn() != null && mapping.containsKey(defaults.nameColumn())) {
            mapping.put(defaults.nameColumn(), ObjectTargets.NAME);
        }

        URI chosen = plan == null ? null : plan.getType();
        URI type = chosen != null ? chosen : defaults.suggestedType();
        return new ObjectSheet(name, defaults.nameColumn(), new ArrayList<>(headers), rows, type,
                chosen == null && type != null, plan == null || plan.isIncluded(), mapping);
    }

    /**
     * What is wrong with a sheet's mapping for its type: a column mapped to a property the type
     * does not accept — the importer would refuse every row that has a value there — or two
     * columns feeding a property that holds one value.
     *
     * @param properties the properties of the sheet's type
     */
    public static List<ReportMessage> problemsOf(ObjectSheet sheet, List<TypeProperty> properties) {
        List<ReportMessage> problems = new ArrayList<>();
        Map<String, TypeProperty> byUri = new LinkedHashMap<>();
        properties.forEach(property -> byUri.put(property.uri().toString(), property));

        Map<String, List<String>> columnsByTarget = new LinkedHashMap<>();
        sheet.getMapping().forEach((column, target) -> {
            if (!target.isEmpty()) {
                columnsByTarget.computeIfAbsent(target, key -> new ArrayList<>()).add(column);
            }
        });

        columnsByTarget.forEach((target, columns) -> {
            TypeProperty property = find(byUri, target);
            if (property == null && !ObjectTargets.ALWAYS_ACCEPTED.contains(target)) {
                for (String column : columns) {
                    problems.add(ReportMessage.of(PROBLEM + "unknownProperty",
                                    "Column '" + column + "' is mapped to " + target
                                            + ", which objects of this type do not have.")
                            .with("column", column)
                            .with("property", target));
                }
                return;
            }
            boolean single = property == null ? !ObjectTargets.FACTOR_LEVEL.equals(target) : !property.list();
            if (single && columns.size() > 1) {
                String named = property == null ? target : property.name();
                problems.add(ReportMessage.of(PROBLEM + "singleValued",
                                "Columns " + String.join(", ", columns) + " are all mapped to " + named
                                        + ", which holds one value.")
                        .with("columns", String.join(", ", columns))
                        .with("property", named));
            }
        });
        return problems;
    }

    private static TypeProperty find(Map<String, TypeProperty> byUri, String target) {
        TypeProperty exact = byUri.get(target);
        if (exact != null) {
            return exact;
        }
        for (TypeProperty property : byUri.values()) {
            if (SPARQLDeserializers.compareURIs(property.uri().toString(), target)) {
                return property;
            }
        }
        return null;
    }
}
