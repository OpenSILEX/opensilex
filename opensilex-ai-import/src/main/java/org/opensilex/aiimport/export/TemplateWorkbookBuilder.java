//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.export;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.opensilex.aiimport.export.ExperimentSnapshot.ExportedObject;
import org.opensilex.aiimport.export.ExperimentSnapshot.Level;
import org.opensilex.aiimport.export.ExperimentSnapshot.ObjectType;
import org.opensilex.aiimport.export.ExperimentSnapshot.Observation;
import org.opensilex.aiimport.export.ExperimentSnapshot.Variable;
import org.opensilex.aiimport.profile.custom.DescribedRole;
import org.opensilex.aiimport.profile.custom.ObjectMerger;
import org.opensilex.aiimport.profile.custom.SheetKind;
import org.opensilex.aiimport.template.FileTemplate;
import org.opensilex.aiimport.template.TemplateSheet;
import org.opensilex.aiimport.workbook.HeaderMatcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Writes an experiment in the shape of a template: a sheet for each sheet of the template, the
 * columns in its order, each filled according to the role the template gives it.
 * <p>
 * It is the other direction of an import by template — the workbook it writes is read back by the
 * module with the same template. What the template has no column for, and what the instance
 * cannot answer for a column, is not dropped in silence: the {@code readme} sheet says how many
 * values and which columns were left out.
 *
 * @author Arnaud Charleroy
 */
public class TemplateWorkbookBuilder implements WorkbookExport {

    public static final String README_SHEET = "readme";

    private static final int ROW_WINDOW = 200;
    private static final int MAX_CELL_LENGTH = 32_767;

    /**
     * The roles an experiment's content cannot fill: nothing in what is read says them.
     */
    private static final Set<DescribedRole> NOT_EXPORTED = Set.of(DescribedRole.LOCATION, DescribedRole.PERSON,
            DescribedRole.OBSERVER, DescribedRole.PROJECT);

    private final FileTemplate template;
    private final LocalDate exportedOn;
    private final Set<String> sheetNames = new HashSet<>();
    private final List<String> notExported = new ArrayList<>();

    private SXSSFWorkbook workbook;
    private CellStyle dateStyle;
    private CellStyle dateTimeStyle;

    /**
     * @param exportedOn the day written in the readme
     */
    public TemplateWorkbookBuilder(FileTemplate template, LocalDate exportedOn) {
        this.template = template;
        this.exportedOn = exportedOn;
    }

    @Override
    public byte[] build(ExperimentSnapshot snapshot) throws IOException {
        try (SXSSFWorkbook book = new SXSSFWorkbook(ROW_WINDOW)) {
            workbook = book;
            sheetNames.clear();
            notExported.clear();
            dateStyle = style("yyyy-mm-dd");
            dateTimeStyle = style("yyyy-mm-dd hh:mm");

            Map<URI, String> namesByUri = namesByUri(snapshot);
            Set<URI> exportedVariables = new LinkedHashSet<>();
            for (TemplateSheet sheet : template.getSheets()) {
                SheetKind kind = sheet.kind().orElse(SheetKind.IGNORED);
                if (kind.describesObjects() || kind.holdsData()) {
                    writeObjectsAndData(snapshot, sheet, kind, namesByUri, exportedVariables);
                } else if (kind == SheetKind.FACTORS) {
                    writeFactors(snapshot, sheet);
                } else if (kind == SheetKind.EXPERIMENTS) {
                    writeExperiment(snapshot, sheet);
                } else if (kind != SheetKind.IGNORED) {
                    notExported.add("sheet '" + sheet.getName() + "': nothing of an experiment fills a sheet of "
                            + kind.name().toLowerCase(Locale.ROOT).replace('_', ' ') + ".");
                }
            }
            writeReadme(snapshot, exportedVariables);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            book.write(out);
            book.dispose();
            return out.toByteArray();
        } finally {
            workbook = null;
        }
    }

    //#region objects and observations

    private void writeObjectsAndData(ExperimentSnapshot snapshot, TemplateSheet template, SheetKind kind,
                                     Map<URI, String> namesByUri, Set<URI> exportedVariables) {
        Map<String, DescribedRole> roles = template.roles();
        Map<String, ExportedObject> objects = objectsOf(snapshot, template);
        Map<String, URI> variablesByHeader = variablesByHeader(snapshot, template, roles);
        leftOut(template, roles, variablesByHeader);

        Sheet sheet = newSheet(template.getName());
        writeRow(sheet, 0, template.getHeaders());
        int index = 1;
        if (kind.holdsData()) {
            for (Map.Entry<String, Map<LocalDateTime, Map<URI, List<Object>>>> target : observationsOf(snapshot,
                    namesByUri, variablesByHeader).entrySet()) {
                ExportedObject object = objects.get(target.getKey());
                for (Map.Entry<LocalDateTime, Map<URI, List<Object>>> moment : target.getValue().entrySet()) {
                    int repeats = moment.getValue().values().stream().mapToInt(List::size).max().orElse(0);
                    for (int repeat = 0; repeat < repeats; repeat++) {
                        List<Object> row = new ArrayList<>();
                        for (String header : template.getHeaders()) {
                            row.add(observationCell(snapshot, header, roles.get(header), target.getKey(), object,
                                    moment.getKey(), moment.getValue(), variablesByHeader.get(header), repeat,
                                    exportedVariables, template));
                        }
                        writeRow(sheet, index++, row);
                    }
                }
            }
        } else {
            for (ExportedObject object : objects.values()) {
                List<Object> row = new ArrayList<>();
                for (String header : template.getHeaders()) {
                    row.add(objectCell(snapshot, header, roles.get(header), object, template));
                }
                writeRow(sheet, index++, row);
            }
        }
    }

    private Object observationCell(ExperimentSnapshot snapshot, String header, DescribedRole role, String target,
                                   ExportedObject object, LocalDateTime moment, Map<URI, List<Object>> values,
                                   URI variable, int repeat, Set<URI> exportedVariables, TemplateSheet template) {
        if (role == DescribedRole.OBJECT) {
            return target;
        }
        if (role == DescribedRole.DATE) {
            return dateOnly(snapshot, moment) ? moment.toLocalDate() : moment;
        }
        if (role == DescribedRole.VARIABLE) {
            List<Object> own = variable == null ? null : values.get(variable);
            if (own == null || repeat >= own.size()) {
                return null;
            }
            exportedVariables.add(variable);
            return own.get(repeat);
        }
        return object == null ? null : objectCell(snapshot, header, role, object, template);
    }

    private boolean dateOnly(ExperimentSnapshot snapshot, LocalDateTime moment) {
        return snapshot.getObservations().stream().filter(observation -> moment.equals(observation.date()))
                .allMatch(Observation::dateOnly);
    }

    /**
     * What the experiment says for one column of an object, by the role the template gives it.
     */
    private Object objectCell(ExperimentSnapshot snapshot, String header, DescribedRole role, ExportedObject object,
                              TemplateSheet template) {
        if (role == null) {
            return null;
        }
        switch (role) {
            case OBJECT:
                return object.name();
            case GERMPLASM:
                return object.germplasm();
            case FACTOR_LEVEL:
                return levelOf(snapshot, header, object);
            case PARENT_OBJECT:
                return object.parent();
            case POSITION:
                return "y".equals(ObjectMerger.axis(header)) ? object.y() : object.x();
            case OBJECT_PROPERTY:
                return propertyOf(template, header, object);
            case COMMENT:
                return object.comment();
            case TRIAL:
                return snapshot.getName();
            default:
                return null;
        }
    }

    /**
     * The level of the factor the header names, among the levels the object carries. The header of a
     * level column is the factor's name, whatever case or accents it is written with.
     */
    private static String levelOf(ExperimentSnapshot snapshot, String header, ExportedObject object) {
        String wanted = HeaderMatcher.normalize(header);
        for (Level level : snapshot.getLevels()) {
            if (level.factor() != null && HeaderMatcher.normalize(level.factor()).equals(wanted)
                    && object.levels().contains(level.name())) {
                return level.name();
            }
        }
        return null;
    }

    private static String propertyOf(TemplateSheet template, String header, ExportedObject object) {
        String property = template.getProperties().get(header);
        if (property == null || property.isBlank()) {
            return null;
        }
        return object.properties().get(URI.create(property));
    }

    private Map<String, ExportedObject> objectsOf(ExperimentSnapshot snapshot, TemplateSheet template) {
        Map<String, ExportedObject> objects = new TreeMap<>();
        for (ObjectType type : snapshot.getObjectTypes()) {
            if (template.getType() == null || template.getType().equals(type.uri())) {
                type.objects().forEach(object -> objects.putIfAbsent(object.name(), object));
            }
        }
        return objects;
    }

    /**
     * Observations by target name, then moment, then variable, in the order of the values. Only the
     * variables the template has a column for are kept: the others are counted in the readme.
     */
    private Map<String, Map<LocalDateTime, Map<URI, List<Object>>>> observationsOf(
            ExperimentSnapshot snapshot, Map<URI, String> namesByUri, Map<String, URI> variablesByHeader) {
        Set<URI> wanted = new HashSet<>(variablesByHeader.values());
        Map<String, Map<LocalDateTime, Map<URI, List<Object>>>> byTarget = new TreeMap<>();
        for (Observation observation : snapshot.getObservations()) {
            String target = namesByUri.get(observation.target());
            if (target == null || !wanted.contains(observation.variable()) || observation.date() == null) {
                continue;
            }
            byTarget.computeIfAbsent(target, name -> new TreeMap<>())
                    .computeIfAbsent(observation.date(), moment -> new LinkedHashMap<>())
                    .computeIfAbsent(observation.variable(), variable -> new ArrayList<>())
                    .add(observation.value());
        }
        return byTarget;
    }

    /**
     * The variable each column of variables stands for: the one the template names by its URI, and
     * failing that the one whose name is the header.
     */
    private static Map<String, URI> variablesByHeader(ExperimentSnapshot snapshot, TemplateSheet template,
                                                      Map<String, DescribedRole> roles) {
        Map<String, URI> byHeader = new LinkedHashMap<>();
        for (Map.Entry<String, DescribedRole> role : roles.entrySet()) {
            if (role.getValue() != DescribedRole.VARIABLE) {
                continue;
            }
            String header = role.getKey();
            String chosen = template.getProperties().get(header);
            for (Map.Entry<URI, Variable> variable : snapshot.getVariables().entrySet()) {
                boolean byUri = chosen != null && chosen.equals(variable.getKey().toString());
                boolean byName = (chosen == null || chosen.isBlank()) && variable.getValue().code() != null
                        && HeaderMatcher.normalize(variable.getValue().code()).equals(HeaderMatcher.normalize(header));
                if (byUri || byName) {
                    byHeader.put(header, variable.getKey());
                    break;
                }
            }
        }
        return byHeader;
    }

    private static Map<URI, String> namesByUri(ExperimentSnapshot snapshot) {
        Map<URI, String> names = new LinkedHashMap<>();
        snapshot.getObjectTypes().forEach(type -> type.objects().forEach(object -> names.put(object.uri(), object.name())));
        snapshot.getFacilities().forEach(facility -> names.putIfAbsent(facility.uri(), facility.name()));
        return names;
    }

    /**
     * Notes the columns of a sheet nothing can fill, so that the readme can say they were left empty.
     */
    private void leftOut(TemplateSheet sheet, Map<String, DescribedRole> roles, Map<String, URI> variablesByHeader) {
        List<String> empty = new ArrayList<>();
        for (String header : sheet.getHeaders()) {
            DescribedRole role = roles.get(header);
            boolean unfillable = role == null || role == DescribedRole.UNREAD || NOT_EXPORTED.contains(role)
                    || role == DescribedRole.VARIABLE && !variablesByHeader.containsKey(header);
            if (unfillable) {
                empty.add(header);
            }
        }
        if (!empty.isEmpty()) {
            notExported.add("sheet '" + sheet.getName() + "': columns left empty, the experiment has nothing for "
                    + "them: " + String.join(", ", empty) + ".");
        }
    }

    //#endregion

    //#region the experiment

    /**
     * The experiment itself, on one row: its name, objective, description, dates and projects. What the
     * snapshot does not read, such as its alternative name, is left empty and said in the readme.
     */
    private void writeExperiment(ExperimentSnapshot snapshot, TemplateSheet template) {
        Map<String, DescribedRole> roles = template.roles();
        Sheet sheet = newSheet(template.getName());
        writeRow(sheet, 0, template.getHeaders());
        List<Object> row = new ArrayList<>();
        List<String> empty = new ArrayList<>();
        for (String header : template.getHeaders()) {
            Object value = experimentCell(snapshot, roles.get(header));
            if (value == null) {
                empty.add(header);
            }
            row.add(value);
        }
        writeRow(sheet, 1, row);
        if (!empty.isEmpty()) {
            notExported.add("sheet '" + template.getName() + "': columns left empty, the experiment has nothing for "
                    + "them: " + String.join(", ", empty) + ".");
        }
    }

    private static Object experimentCell(ExperimentSnapshot snapshot, DescribedRole role) {
        if (role == null) {
            return null;
        }
        switch (role) {
            case TRIAL:
                return snapshot.getName();
            case EXPERIMENT_OBJECTIVE:
                return snapshot.getObjective();
            case EXPERIMENT_DESCRIPTION:
                return snapshot.getDescription();
            case EXPERIMENT_START:
                return snapshot.getStartDate();
            case EXPERIMENT_END:
                return snapshot.getEndDate();
            case PROJECT:
                return snapshot.getProjects().isEmpty() ? null : String.join(", ", snapshot.getProjects());
            default:
                return null;
        }
    }

    //#endregion

    //#region factors

    private void writeFactors(ExperimentSnapshot snapshot, TemplateSheet template) {
        Map<String, DescribedRole> roles = template.roles();
        Sheet sheet = newSheet(template.getName());
        writeRow(sheet, 0, template.getHeaders());
        int index = 1;
        for (Level level : snapshot.getLevels()) {
            List<Object> row = new ArrayList<>();
            for (String header : template.getHeaders()) {
                row.add(levelCell(level, roles.get(header)));
            }
            writeRow(sheet, index++, row);
        }
        // The platform keeps one description per level, and none of its own for the factor here.
        List<String> empty = new ArrayList<>();
        roles.forEach((header, role) -> {
            if (role == DescribedRole.FACTOR_LEVEL_NAME || role == DescribedRole.FACTOR_DESCRIPTION) {
                empty.add(header);
            }
        });
        if (!empty.isEmpty()) {
            notExported.add("sheet '" + template.getName() + "': columns left empty, the experiment keeps a level's "
                    + "name and description together: " + String.join(", ", empty) + ".");
        }
    }

    private static Object levelCell(Level level, DescribedRole role) {
        if (role == DescribedRole.FACTOR) {
            return level.factor();
        }
        if (role == DescribedRole.FACTOR_LEVEL) {
            return level.name();
        }
        if (role == DescribedRole.FACTOR_LEVEL_DESCRIPTION || role == DescribedRole.COMMENT) {
            return level.description();
        }
        return null;
    }

    //#endregion

    //#region the readme

    /**
     * Says what the workbook is and what was left out: the experiment, the template, the day, the
     * columns nothing could fill, and the values of variables the template has no column for.
     */
    private void writeReadme(ExperimentSnapshot snapshot, Set<URI> exportedVariables) {
        Sheet sheet = newSheet(README_SHEET);
        List<String> lines = new ArrayList<>();
        lines.add("Experiment: " + snapshot.getName());
        lines.add("Written in the shape of the template: " + template.getName());
        lines.add("Exported on: " + exportedOn);
        lines.addAll(notExported);

        Map<URI, Long> unexported = new LinkedHashMap<>();
        for (Observation observation : snapshot.getObservations()) {
            if (!exportedVariables.contains(observation.variable())) {
                unexported.merge(observation.variable(), 1L, Long::sum);
            }
        }
        if (!unexported.isEmpty()) {
            long values = unexported.values().stream().mapToLong(Long::longValue).sum();
            List<String> names = new ArrayList<>();
            unexported.keySet().forEach(variable -> names.add(snapshot.getVariables().containsKey(variable)
                    ? snapshot.getVariables().get(variable).code()
                    : variable.toString()));
            lines.add(values + " values of variables the template has no column for were not exported: "
                    + String.join(", ", names) + ".");
        }
        if (snapshot.getObservationsWithoutTarget() > 0) {
            lines.add(snapshot.getObservationsWithoutTarget() + " values concern neither an object of the "
                    + "experiment nor one of its facilities, and were not exported.");
        }
        for (int i = 0; i < lines.size(); i++) {
            writeRow(sheet, i, List.of(lines.get(i)));
        }
    }

    //#endregion

    //#region cells and sheets

    private Sheet newSheet(String wanted) {
        String safe = WorkbookUtil.createSafeSheetName(wanted);
        String name = safe;
        for (int i = 2; sheetNames.contains(name.toLowerCase(Locale.ROOT)); i++) {
            String suffix = "_" + i;
            name = safe.substring(0, Math.min(safe.length(), 31 - suffix.length())) + suffix;
        }
        sheetNames.add(name.toLowerCase(Locale.ROOT));
        return workbook.createSheet(name);
    }

    private void writeRow(Sheet sheet, int index, List<?> values) {
        Row row = sheet.createRow(index);
        for (int i = 0; i < values.size(); i++) {
            cell(row, i, values.get(i));
        }
    }

    private void cell(Row row, int column, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Number number) {
            row.createCell(column).setCellValue(number.doubleValue());
        } else if (value instanceof Boolean bool) {
            row.createCell(column).setCellValue(bool);
        } else if (value instanceof LocalDate date) {
            Cell cell = row.createCell(column);
            cell.setCellValue(date);
            cell.setCellStyle(dateStyle);
        } else if (value instanceof LocalDateTime dateTime) {
            Cell cell = row.createCell(column);
            cell.setCellValue(dateTime);
            cell.setCellStyle(dateTimeStyle);
        } else {
            String text = value.toString();
            if (!text.isEmpty()) {
                row.createCell(column).setCellValue(text.length() > MAX_CELL_LENGTH
                        ? text.substring(0, MAX_CELL_LENGTH) : text);
            }
        }
    }

    private CellStyle style(String format) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(format));
        return style;
    }

    //#endregion
}
