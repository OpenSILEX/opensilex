//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.export;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.opensilex.aiimport.export.ExperimentSnapshot.ExportedObject;
import org.opensilex.aiimport.export.ExperimentSnapshot.Facility;
import org.opensilex.aiimport.export.ExperimentSnapshot.Level;
import org.opensilex.aiimport.export.ExperimentSnapshot.ObjectType;
import org.opensilex.aiimport.export.ExperimentSnapshot.Observation;
import org.opensilex.aiimport.export.ExperimentSnapshot.Variable;
import org.opensilex.aiimport.profile.star.StarDictionary;
import org.opensilex.aiimport.profile.star.StarProfile;
import org.opensilex.aiimport.profile.star.StarSheets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Writes an experiment as a STAR workbook: the sheets the format defines, in its column names, with
 * a dictionary describing every column written.
 * <p>
 * The workbook is the one {@link StarProfile} reads: the same sheet prefixes, the same column names
 * — taken from the profile's own constants, so the two cannot drift apart — and the same way of
 * pointing from one sheet to another, by name. An exported experiment can therefore be read back
 * by this module, and by anything else that reads STAR.
 * <p>
 * What the format has no column for is written anyway, in columns added the way the standard allows
 * — "add columns freely, and declare them in the dictionary": the URIs of origin, the type of the
 * objects of each design sheet, the properties of that type.
 * <p>
 * One design sheet per type of object, since a STAR design sheet holds one kind of experimental
 * unit; and one data sheet per type as well, with the facility's observations — the weather, in a
 * STAR file — in {@value #FACILITY_DATA_SHEET}.
 * <p>
 * Streamed: rows leave memory as they are written, so the size of the workbook is bounded by what
 * the snapshot holds, not twice that.
 * <p>
 * One builder per workbook: it keeps the columns it has described while it writes.
 *
 * @author Arnaud Charleroy
 */
public class StarWorkbookBuilder {

    public static final String README_SHEET = "readme";
    public static final String FIELD_SHEET = StarSheets.FIELD_PREFIX;
    public static final String FACILITY_DATA_SHEET = StarSheets.DATA_PREFIX + "meteo";

    //#region the columns this module adds to the standard

    public static final String COLUMN_EXPERIMENT_URI = "expe_uri";
    public static final String COLUMN_FIELD_URI = "field_uri";
    public static final String COLUMN_FACTOR = StarProfile.COLUMN_FACTOR;
    public static final String COLUMN_OBJECT_TYPE = StarProfile.COLUMN_OBJECT_TYPE;
    public static final String COLUMN_OBJECT_URI = "object_uri";
    public static final String COLUMN_PARENT = StarProfile.COLUMN_PARENT;

    //#endregion

    /**
     * The ELOA class the reference template gives every variable.
     */
    static final String ELOA_VARIABLE = "Variable";

    private static final List<String> DICTIONARY_HEADERS = Arrays.asList(
            "nom", "description_fr", "trait", "method", "unit", "Rclass", "uri", "has_eloa_class");

    /**
     * The facility columns the standard names; any other property of the facility follows them.
     */
    private static final List<String> STANDARD_FIELD_PROPERTIES = Arrays.asList(
            StarProfile.COLUMN_ROW_SPACING, StarProfile.COLUMN_PLANT_SPACING, StarProfile.COLUMN_INSEE);

    private static final int MAX_CELL_LENGTH = 32_767;

    /**
     * Rows kept in memory per sheet before they are flushed to disk.
     */
    private static final int ROW_WINDOW = 200;

    private final LocalDate exportedOn;

    /**
     * Every metadata column written, by name, with its description: the dictionary sheet is written
     * last, from this.
     */
    private final Map<String, StarMetadataDescriptions.Entry> described = new TreeMap<>();

    private final Set<String> sheetNames = new HashSet<>();

    private SXSSFWorkbook workbook;
    private CellStyle dateStyle;
    private CellStyle dateTimeStyle;

    /**
     * @param exportedOn the day written in the readme
     */
    public StarWorkbookBuilder(LocalDate exportedOn) {
        this.exportedOn = exportedOn;
    }

    /**
     * @return the workbook, as the bytes of an {@code .xlsx} file
     */
    public byte[] build(ExperimentSnapshot snapshot) throws IOException {
        try (SXSSFWorkbook book = new SXSSFWorkbook(ROW_WINDOW)) {
            workbook = book;
            dateStyle = style("yyyy-mm-dd");
            dateTimeStyle = style("yyyy-mm-dd hh:mm");

            writeReadme(snapshot);
            writeExperiment(snapshot);
            writeFields(snapshot);
            writeLevels(snapshot);
            Map<ObjectType, String> idColumns = writeDesignSheets(snapshot);
            writeDataSheets(snapshot, idColumns);
            writeVariables(snapshot);
            writeMetadata();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            book.write(out);
            book.dispose();
            return out.toByteArray();
        } finally {
            workbook = null;
        }
    }

    //#region the sheets

    private void writeReadme(ExperimentSnapshot snapshot) {
        Sheet sheet = newSheet(README_SHEET);
        List<String> lines = new ArrayList<>(Arrays.asList(
                "Modèle de données STAR",
                "Classeur exporté d'OpenSILEX le " + exportedOn + " : expérimentation « "
                        + snapshot.getName() + " » (" + snapshot.getUri() + ").",
                "La feuille « " + StarSheets.EXPERIMENT_SHEET + " » décrit l'expérimentation, « "
                        + FIELD_SHEET + " » ses installations, « " + StarSheets.TREATMENT_SHEET
                        + " » les modalités de ses facteurs.",
                "Chaque feuille « " + StarSheets.EXPERIMENTAL_DESIGN_PREFIX + "… » regroupe les objets "
                        + "scientifiques d'un même type ; chaque feuille « " + StarSheets.DATA_PREFIX
                        + "… » leurs observations, une colonne par variable décrite dans « "
                        + StarDictionary.VARIABLES_SHEET + " ».",
                "Les colonnes ajoutées au standard — URI d'origine, type des objets, propriétés de "
                        + "ce type — sont décrites dans « " + StarDictionary.METADATA_SHEET + " »."));
        if (snapshot.getObservationsWithoutTarget() > 0) {
            lines.add(snapshot.getObservationsWithoutTarget() + " valeur(s) portant sur une cible qui "
                    + "n'est ni un objet ni une installation de l'expérimentation n'ont pas été "
                    + "exportées.");
        }
        for (int i = 0; i < lines.size(); i++) {
            cell(sheet.createRow(i), 0, lines.get(i));
        }
    }

    /**
     * One row per project, organisation or contact, whichever there are most of: STAR reads each
     * column's distinct values, so a list in one cell would read as a single name.
     */
    private void writeExperiment(ExperimentSnapshot snapshot) {
        List<String> headers = Arrays.asList(StarProfile.COLUMN_ORGANIZATION,
                StarProfile.COLUMN_SUBORGANIZATION, StarProfile.COLUMN_EMAIL,
                StarProfile.COLUMN_EXPERIMENT, StarProfile.COLUMN_OBJECTIVE,
                StarProfile.COLUMN_DESCRIPTION, StarProfile.COLUMN_START_DATE,
                StarProfile.COLUMN_END_DATE, StarProfile.COLUMN_DESIGN, StarProfile.COLUMN_PROJECT,
                COLUMN_EXPERIMENT_URI);
        Sheet sheet = newSheet(StarSheets.EXPERIMENT_SHEET, headers);

        int rows = Math.max(1, max(snapshot.getProjects(), snapshot.getOrganizations(),
                snapshot.getSuborganizations(), snapshot.getEmails()));
        for (int i = 0; i < rows; i++) {
            writeRow(sheet, i + 1, Arrays.asList(
                    at(snapshot.getOrganizations(), i), at(snapshot.getSuborganizations(), i),
                    at(snapshot.getEmails(), i), snapshot.getName(), snapshot.getObjective(),
                    snapshot.getDescription(), snapshot.getStartDate(), snapshot.getEndDate(), null,
                    at(snapshot.getProjects(), i), uri(snapshot.getUri())));
        }
    }

    /**
     * The facilities, named the same in the identifier and the name columns: the data sheets and
     * the design sheets point to a field by its identifier, and a name is what reads back.
     */
    private void writeFields(ExperimentSnapshot snapshot) {
        Set<String> extraColumns = new TreeSet<>();
        for (Facility facility : snapshot.getFacilities()) {
            facility.properties().keySet().stream()
                    .filter(column -> !STANDARD_FIELD_PROPERTIES.contains(column))
                    .forEach(extraColumns::add);
        }
        List<String> headers = new ArrayList<>(Arrays.asList(StarProfile.COLUMN_FIELD,
                StarProfile.COLUMN_FIELD_NAME, StarProfile.COLUMN_CULTIVAR,
                StarProfile.COLUMN_ROW_SPACING, StarProfile.COLUMN_PLANT_SPACING,
                StarProfile.COLUMN_TOWN, StarProfile.COLUMN_INSEE, StarProfile.COLUMN_LATITUDE,
                StarProfile.COLUMN_LONGITUDE, COLUMN_FIELD_URI));
        for (String column : extraColumns) {
            describe(column, "Propriété de l'installation", "");
        }
        headers.addAll(extraColumns);
        Sheet sheet = newSheet(FIELD_SHEET, headers);

        String cultivar = soleCultivar(snapshot);
        int index = 1;
        for (Facility facility : snapshot.getFacilities()) {
            List<Object> row = new ArrayList<>(Arrays.asList(facility.name(), facility.name(), cultivar,
                    facility.properties().get(StarProfile.COLUMN_ROW_SPACING),
                    facility.properties().get(StarProfile.COLUMN_PLANT_SPACING), facility.town(),
                    facility.properties().get(StarProfile.COLUMN_INSEE), facility.latitude(),
                    facility.longitude(), uri(facility.uri())));
            extraColumns.forEach(column -> row.add(facility.properties().get(column)));
            writeRow(sheet, index++, row);
        }
    }

    /**
     * The levels, their code being the level's name: that is what the design sheets write in their
     * treatment column, and what the platform matches a treatment on when the file is imported.
     */
    private void writeLevels(ExperimentSnapshot snapshot) {
        Sheet sheet = newSheet(StarSheets.TREATMENT_SHEET, Arrays.asList(StarProfile.COLUMN_EXPERIMENT,
                StarProfile.COLUMN_TREATMENT, StarProfile.COLUMN_TREATMENT_NAME,
                StarProfile.COLUMN_TREATMENT_DESCRIPTION, COLUMN_FACTOR));
        int index = 1;
        for (Level level : snapshot.getLevels()) {
            writeRow(sheet, index++, Arrays.asList(snapshot.getName(), level.name(), level.description(),
                    null, level.factor()));
        }
    }

    /**
     * One sheet per type. A column the standard names is written under that name for a plot, and
     * under the same pattern for any other type — {@code plant_id} beside {@code plot_id} — so the
     * shape of every design sheet is the one STAR gives its plots.
     *
     * @return the identifier column of each type, which the data sheets use too
     */
    private Map<ObjectType, String> writeDesignSheets(ExperimentSnapshot snapshot) {
        Map<ObjectType, String> idColumns = new LinkedHashMap<>();
        for (ObjectType type : snapshot.getObjectTypes()) {
            String prefix = slug(type.localName());
            String idColumn = prefix + "_id";
            idColumns.put(type, idColumn);

            List<ExportedObject> objects = type.objects();
            boolean levels = objects.stream().anyMatch(object -> !object.levels().isEmpty());
            boolean parents = objects.stream().anyMatch(object -> object.parent() != null);
            boolean germplasm = objects.stream().anyMatch(object -> object.germplasm() != null);
            boolean positions = objects.stream().anyMatch(object -> object.x() != null || object.y() != null);
            boolean comments = objects.stream().anyMatch(object -> object.comment() != null);

            List<String> headers = new ArrayList<>();
            headers.add(idColumn);
            describeObjectColumn(idColumn, "Identifiant de l'objet", type);
            if (levels) {
                headers.add(StarProfile.COLUMN_TREATMENT);
            }
            if (parents) {
                headers.add(COLUMN_PARENT);
            }
            if (germplasm) {
                headers.add(StarProfile.COLUMN_CULTIVAR);
            }
            if (positions) {
                headers.add(prefix + "_x");
                headers.add(prefix + "_y");
                describeObjectColumn(prefix + "_x", "Position relative x de l'objet", type);
                describeObjectColumn(prefix + "_y", "Position relative y de l'objet", type);
            }
            if (comments) {
                headers.add(prefix + "_desc");
                describeObjectColumn(prefix + "_desc", "Description ou commentaire sur l'objet", type);
            }
            headers.add(COLUMN_OBJECT_TYPE);
            headers.add(COLUMN_OBJECT_URI);
            type.propertyNames().forEach((property, column) -> {
                headers.add(column);
                describe(column, "Propriété des objets de type « " + type.label() + " »", property.toString());
            });

            Sheet sheet = newSheet(uniqueSheetName(StarSheets.EXPERIMENTAL_DESIGN_PREFIX + prefix), headers);
            int index = 1;
            for (ExportedObject object : objects) {
                List<Object> row = new ArrayList<>();
                row.add(object.name());
                if (levels) {
                    row.add(String.join(" ", object.levels()));
                }
                if (parents) {
                    row.add(object.parent());
                }
                if (germplasm) {
                    row.add(object.germplasm());
                }
                if (positions) {
                    row.add(object.x());
                    row.add(object.y());
                }
                if (comments) {
                    row.add(object.comment());
                }
                row.add(uri(type.uri()));
                row.add(uri(object.uri()));
                type.propertyNames().keySet().forEach(property -> row.add(object.properties().get(property)));
                writeRow(sheet, index++, row);
            }
        }
        return idColumns;
    }

    /**
     * The observations, one data sheet per type of object and one for the facilities, one column
     * per variable.
     * <p>
     * Values of one variable on one target at one date are repetitions — several leaves read on the
     * same plot the same day — and become as many rows, as the reference template writes them.
     */
    private void writeDataSheets(ExperimentSnapshot snapshot, Map<ObjectType, String> idColumns) {
        Map<URI, Target> targets = new LinkedHashMap<>();
        Map<String, DataSheet> sheets = new LinkedHashMap<>();
        for (ObjectType type : snapshot.getObjectTypes()) {
            DataSheet sheet = new DataSheet(StarSheets.DATA_PREFIX + slug(type.localName()),
                    idColumns.get(type), StarProfile.COLUMN_OBSERVATION_DATE);
            sheets.put(sheet.name, sheet);
            type.objects().forEach(object -> targets.put(object.uri(), new Target(sheet, object.name())));
        }
        DataSheet facilities = new DataSheet(FACILITY_DATA_SHEET, StarProfile.COLUMN_FIELD,
                StarProfile.COLUMN_METEO_DATETIME);
        sheets.put(facilities.name, facilities);
        snapshot.getFacilities().forEach(facility ->
                targets.put(facility.uri(), new Target(facilities, facility.name())));

        for (Observation observation : snapshot.getObservations()) {
            Target target = targets.get(observation.target());
            Variable variable = snapshot.getVariables().get(observation.variable());
            if (target == null || variable == null) {
                continue;
            }
            target.sheet.variables.add(variable.code());
            target.sheet.values
                    .computeIfAbsent(new RowKey(target.name, observation.date(), observation.dateOnly()),
                            key -> new LinkedHashMap<>())
                    .computeIfAbsent(variable.code(), code -> new ArrayList<>())
                    .add(observation.value());
        }

        for (DataSheet data : sheets.values()) {
            if (!data.values.isEmpty()) {
                writeDataSheet(data);
            }
        }
    }

    private void writeDataSheet(DataSheet data) {
        List<String> headers = new ArrayList<>();
        boolean facility = FACILITY_DATA_SHEET.equals(data.name);
        // The weather sheet of the template names the field first, the observation sheets the date.
        headers.add(facility ? data.idColumn : data.dateColumn);
        headers.add(facility ? data.dateColumn : data.idColumn);
        List<String> variables = new ArrayList<>(data.variables);
        Sheet sheet = newSheet(uniqueSheetName(data.name), headers, variables);

        int index = 1;
        for (Map.Entry<RowKey, Map<String, List<Object>>> group : data.values.entrySet()) {
            RowKey key = group.getKey();
            int repetitions = group.getValue().values().stream().mapToInt(List::size).max().orElse(1);
            Object date = key.dateOnly ? key.date.toLocalDate() : key.date;
            for (int repetition = 0; repetition < repetitions; repetition++) {
                List<Object> row = new ArrayList<>();
                row.add(facility ? key.target : date);
                row.add(facility ? date : key.target);
                for (String variable : variables) {
                    row.add(at(group.getValue().get(variable), repetition));
                }
                writeRow(sheet, index++, row);
            }
        }
    }

    private void writeVariables(ExperimentSnapshot snapshot) {
        Sheet sheet = newSheet(StarDictionary.VARIABLES_SHEET);
        writeRow(sheet, 0, new ArrayList<>(DICTIONARY_HEADERS));
        List<Variable> variables = new ArrayList<>(snapshot.getVariables().values());
        variables.sort(Comparator.comparing(Variable::code, String.CASE_INSENSITIVE_ORDER));
        int index = 1;
        for (Variable variable : variables) {
            writeRow(sheet, index++, Arrays.asList(variable.code(), variable.description(),
                    variable.trait(), variable.method(), variable.unit(), rClassOf(variable.datatype()),
                    variable.uri(), ELOA_VARIABLE));
        }
    }

    private void writeMetadata() {
        Sheet sheet = newSheet(StarDictionary.METADATA_SHEET);
        writeRow(sheet, 0, new ArrayList<>(DICTIONARY_HEADERS));
        int index = 1;
        for (StarMetadataDescriptions.Entry entry : described.values()) {
            writeRow(sheet, index++, Arrays.asList(entry.name(), entry.description(), null, null,
                    entry.unit(), entry.rClass(), entry.uri(), entry.eloaClass()));
        }
    }

    //#endregion

    //#region describing columns

    /**
     * Records a metadata column for the dictionary, described as the standard describes it.
     */
    private void describe(String column) {
        StarMetadataDescriptions.get(column).ifPresent(entry -> described.putIfAbsent(column, entry));
    }

    /**
     * Records a column the standard does not know, unless it turns out to know it after all.
     */
    private void describe(String column, String description, String uri) {
        described.putIfAbsent(column, StarMetadataDescriptions.get(column).orElse(
                new StarMetadataDescriptions.Entry(column, description, "", "character", uri, "")));
    }

    private void describeObjectColumn(String column, String what, ObjectType type) {
        describe(column, what + " (type « " + type.label() + " ») au sein de l'expérimentation", "");
    }

    /**
     * The R class the format uses for an XSD datatype; text for anything it has no class for.
     */
    static String rClassOf(String datatype) {
        if (datatype == null) {
            return "character";
        }
        if (datatype.equals(XSDDatatype.XSDdecimal.getURI()) || datatype.equals(XSDDatatype.XSDdouble.getURI())
                || datatype.equals(XSDDatatype.XSDfloat.getURI())) {
            return "numeric";
        }
        if (datatype.equals(XSDDatatype.XSDinteger.getURI()) || datatype.equals(XSDDatatype.XSDint.getURI())
                || datatype.equals(XSDDatatype.XSDlong.getURI())) {
            return "integer";
        }
        if (datatype.equals(XSDDatatype.XSDdate.getURI())) {
            return "date";
        }
        if (datatype.equals(XSDDatatype.XSDdateTime.getURI())) {
            return "datetime";
        }
        if (datatype.equals(XSDDatatype.XSDboolean.getURI())) {
            return "logical";
        }
        return "character";
    }

    //#endregion

    //#region writing cells

    private Sheet newSheet(String name) {
        sheetNames.add(name.toLowerCase(Locale.ROOT));
        return workbook.createSheet(name);
    }

    private Sheet newSheet(String name, List<String> metadataHeaders) {
        return newSheet(name, metadataHeaders, List.of());
    }

    /**
     * A sheet and its header row: the metadata columns first, described in the dictionary, then the
     * variables, which the variables dictionary describes.
     */
    private Sheet newSheet(String name, List<String> metadataHeaders, List<String> variableHeaders) {
        Sheet sheet = newSheet(name);
        List<Object> header = new ArrayList<>(metadataHeaders);
        header.addAll(variableHeaders);
        writeRow(sheet, 0, header);
        metadataHeaders.forEach(this::describe);
        return sheet;
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
        } else if (value instanceof Date date) {
            cell(row, column, LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC));
        } else {
            String text = value.toString();
            if (text.isEmpty()) {
                return;
            }
            row.createCell(column).setCellValue(text.length() > MAX_CELL_LENGTH
                    ? text.substring(0, MAX_CELL_LENGTH)
                    : text);
        }
    }

    private CellStyle style(String format) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(format));
        return style;
    }

    /**
     * A sheet name Excel accepts — 31 characters, no reserved character — and not already taken.
     */
    private String uniqueSheetName(String wanted) {
        String safe = WorkbookUtil.createSafeSheetName(wanted);
        String name = safe;
        for (int i = 2; sheetNames.contains(name.toLowerCase(Locale.ROOT)); i++) {
            String suffix = "_" + i;
            name = safe.substring(0, Math.min(safe.length(), 31 - suffix.length())) + suffix;
        }
        return name;
    }

    //#endregion

    //#region helpers

    /**
     * The cultivar of the field, when all its objects share one: the reference template states it
     * once, on the field, and a trial on several varieties states it per plot instead.
     */
    private static String soleCultivar(ExperimentSnapshot snapshot) {
        Set<String> cultivars = new LinkedHashSet<>();
        snapshot.getObjectTypes().forEach(type -> type.objects().stream()
                .map(ExportedObject::germplasm)
                .filter(Objects::nonNull)
                .forEach(cultivars::add));
        return cultivars.size() == 1 ? cultivars.iterator().next() : null;
    }

    /**
     * The last segment of a type, as a column prefix: {@code Plot} becomes {@code plot}.
     */
    static String slug(String localName) {
        String slug = localName == null ? "" : localName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return slug.isEmpty() ? "object" : slug;
    }

    @SafeVarargs
    private static int max(List<String>... lists) {
        int max = 0;
        for (List<String> list : lists) {
            max = Math.max(max, list.size());
        }
        return max;
    }

    private static <T> T at(List<T> list, int index) {
        return list != null && index < list.size() ? list.get(index) : null;
    }

    private static String uri(URI uri) {
        return uri == null ? null : uri.toString();
    }

    private record Target(DataSheet sheet, String name) {
    }

    /**
     * One row group: a target at a date. Sorted by target, then date, so a sheet reads plot by plot.
     */
    private record RowKey(String target, LocalDateTime date, boolean dateOnly) implements Comparable<RowKey> {

        private static final Comparator<RowKey> ORDER = Comparator
                .comparing(RowKey::target, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(RowKey::date);

        @Override
        public int compareTo(RowKey other) {
            return ORDER.compare(this, other);
        }
    }

    private static final class DataSheet {
        private final String name;
        private final String idColumn;
        private final String dateColumn;
        private final Set<String> variables = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        private final Map<RowKey, Map<String, List<Object>>> values = new TreeMap<>();

        private DataSheet(String name, String idColumn, String dateColumn) {
            this.name = name;
            this.idColumn = idColumn;
            this.dateColumn = dateColumn;
        }
    }

    //#endregion
}
