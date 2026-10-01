//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.bulk;

import org.opensilex.aiimport.create.SessionFacts;
import org.opensilex.aiimport.create.objects.ObjectSheet;
import org.opensilex.aiimport.create.objects.ObjectSheets;
import org.opensilex.aiimport.create.objects.ObjectValueResolver;
import org.opensilex.aiimport.create.objects.TypeProperties;
import org.opensilex.aiimport.create.objects.TypeProperties.TypeProperty;
import org.opensilex.aiimport.create.rows.PlatformValidationAdapter;
import org.opensilex.aiimport.create.rows.RowError;
import org.opensilex.aiimport.profile.ImportProfileRegistry;
import org.opensilex.aiimport.profile.ObjectRow;
import org.opensilex.aiimport.profile.ObjectTargets;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.resolve.ReportCategory;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorDAO;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.location.dal.LocationModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.scientificObject.bll.ScientificObjectCsvImporterLogic;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.distributed.SparqlMongoTransaction;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.csv.CSVValidationModel;
import org.opensilex.sparql.service.SPARQLService;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Creates the observed objects of a workbook in one pass, through the platform's own importer.
 * <p>
 * The module writes the CSV that {@link ScientificObjectCsvImporterLogic} expects — the same one
 * the scientific-object screen imports — so every rule of the platform applies: factor levels
 * belong to the experiment, the facility hosts it, a name is unique in it. Nothing about objects is
 * validated twice, once here and once there, with the risk of the two disagreeing.
 * <p>
 * Names in the workbook become URIs here: the germplasm and the facility from the report, the
 * factor level from the experiment's own factors. A named value that does not resolve stops the row
 * before the platform is asked — filing a plot without its variety would not be refused by the
 * importer, it would simply lose the information.
 * <p>
 * A position becomes a move dated from the experiment's start: the workbook gives where a plot is,
 * never since when, and a plot does not move during a trial.
 *
 * @author Arnaud Charleroy
 */
public class ScientificObjectBulkImport extends PlatformBulkImport<CSVValidationModel> {

    public static final String EXPERIMENT = "experiment";
    public static final String OBJECT_TYPE = "object_type";

    /**
     * The shared importer engine reads a header line and a description line before the data.
     */
    private static final int HEADER_LINES = 2;

    private static final String UNRESOLVED = "AiImport.proposal.unresolved.";
    private static final String AMBIGUOUS = "AiImport.proposal.ambiguous.";

    private static final URI SCIENTIFIC_OBJECT = URI.create(Oeso.ScientificObject.getURI());

    private final SPARQLService sparql;
    private final MongoDBService nosql;
    private final FileStorageService fs;
    private final AccountModel user;

    public ScientificObjectBulkImport(SPARQLService sparql, MongoDBService nosql,
                                      FileStorageService fs, AccountModel user) {
        this.sparql = sparql;
        this.nosql = nosql;
        this.fs = fs;
        this.user = user;
    }

    /**
     * @return the objects the workbook describes, as its profile reads them
     */
    public static List<ObjectRow> rowsOf(AiImportSession session) {
        return new ImportProfileRegistry().getById(session.getProfileId())
                .map(profile -> profile.extractObjectRows(session.getWorkbook()))
                .orElse(List.of());
    }

    /**
     * One CSV for every sheet taking part: the platform reads the type row by row, so the plots of
     * one sheet and the plants of another go in the same file, each row under its own type, and
     * are validated and written together — all of them, or none.
     * <p>
     * The header is the union of what the sheets write: the columns every object has, then each
     * property a sheet maps a column to — as many times as the sheet mapping it most, since the
     * importer reads a repeated column as a list — then the move columns when a position is given.
     * A row leaves empty the columns of the other sheets' properties.
     */
    @Override
    protected GeneratedCsv generate(AiImportSession session, Map<String, String> values)
            throws Exception {
        URI experiment = URI.create(values.get(EXPERIMENT));
        URI fallbackType = absoluteUri(values.get(OBJECT_TYPE));

        SessionFacts facts = new SessionFacts(session);
        Map<String, URI> germplasm = facts.resolvedUris(ReportCategory.GERMPLASM);
        Map<String, URI> facilities = facts.resolvedUris(ReportCategory.FACILITIES);
        Map<String, URI> levels = factorLevels(experiment);

        GeneratedCsv csv = new GeneratedCsv(HEADER_LINES);
        List<Layout> layouts = layouts(session, fallbackType, csv);

        List<String> header = new ArrayList<>(Arrays.asList("uri", "type", ObjectTargets.NAME,
                ObjectTargets.GERMPLASM, ObjectTargets.FACTOR_LEVEL, Oeso.isHosted.getURI()));
        Map<String, List<Integer>> extraColumns = extraColumns(layouts, header);
        boolean positioned = layouts.stream().anyMatch(Layout::hasPositions);
        int moveStart = header.size();
        if (positioned) {
            header.addAll(Arrays.asList(ScientificObjectCsvImporterLogic.MOVE_START_FIELD_UNIQUE_HEADER,
                    LocationModel.X_FIELD, LocationModel.Y_FIELD));
        }
        csv.addHeaderLine(header).addHeaderLine(header);
        csv.getOrigins().addColumn(ObjectTargets.NAME, "name")
                .addColumn(ObjectTargets.GERMPLASM, "germplasm")
                .addColumn(ObjectTargets.FACTOR_LEVEL, "treatment")
                .addColumn(Oeso.isHosted.getURI(), "facility");
        String start = positioned ? experimentStart(experiment) : null;

        Map<String, String> sheetOfName = namesInThisRun(layouts);
        ObjectValueResolver resolver = valueResolver();

        for (Layout layout : layouts) {
            ObjectSheet sheet = layout.sheet;
            sheet.getMapping().forEach((column, target) -> {
                if (!target.isEmpty()) {
                    csv.getOrigins().addColumn(sheet.getName(), target, column);
                }
            });
            for (ObjectRow row : sheet.getRows()) {
                String[] line = new String[header.size()];
                Arrays.fill(line, "");
                line[1] = layout.type.toString();
                line[2] = row.getName();
                line[3] = resolve(row, layout.value(row, ObjectTargets.GERMPLASM, row.getGermplasm()),
                        germplasm, "germplasm", csv);
                line[4] = resolve(row, layout.value(row, ObjectTargets.FACTOR_LEVEL, row.getFactorLevel()),
                        levels, "factorLevel", csv);
                line[5] = resolve(row, row.getFacility(), facilities, "facility", csv);

                for (Map.Entry<String, List<Integer>> extra : extraColumns.entrySet()) {
                    List<String> columns = sheet.columnsMappedTo(extra.getKey());
                    for (int i = 0; i < columns.size(); i++) {
                        line[extra.getValue().get(i)] = propertyValue(row, columns.get(i), extra.getKey(),
                                layout, experiment, sheetOfName, resolver, csv);
                    }
                }

                String x = layout.value(row, ObjectTargets.X, row.getX());
                String y = layout.value(row, ObjectTargets.Y, row.getY());
                if (positioned && (x != null || y != null)) {
                    line[moveStart] = start;
                    line[moveStart + 1] = nullToEmpty(x);
                    line[moveStart + 2] = nullToEmpty(y);
                }
                csv.addDataLine(row.getSheet(), row.getRow(), Arrays.asList(line));
            }
        }
        return csv;
    }

    /**
     * The sheets taking part, each with its type and the properties that type accepts; a sheet
     * without a type, or whose mapping the type contradicts, stops the creation with its reason
     * before the platform is asked anything.
     */
    private List<Layout> layouts(AiImportSession session, URI fallbackType, GeneratedCsv csv)
            throws Exception {
        TypeProperties typeProperties = typeProperties();
        List<Layout> layouts = new ArrayList<>();
        for (ObjectSheet sheet : ObjectSheets.of(session)) {
            if (!sheet.isIncluded()) {
                continue;
            }
            URI type = sheet.getType() != null ? sheet.getType() : fallbackType;
            if (type == null) {
                csv.getModuleErrors().add(RowError.ofFile(ReportMessage.of(ObjectSheets.PROBLEM + "noType",
                        "No type was chosen for the objects of sheet '" + sheet.getName() + "'.")
                        .with("sheet", sheet.getName())));
                continue;
            }
            if (!typeProperties.isObjectType(type)) {
                csv.getModuleErrors().add(RowError.ofFile(ReportMessage.of(ObjectSheets.PROBLEM + "notObjectType",
                        "The type chosen for sheet '" + sheet.getName() + "' is not a scientific object type.")
                        .with("sheet", sheet.getName())
                        .with("type", type.toString())));
                continue;
            }
            List<TypeProperty> properties = typeProperties.of(type);
            List<ReportMessage> problems = ObjectSheets.problemsOf(sheet, properties);
            if (!problems.isEmpty()) {
                problems.forEach(problem -> csv.getModuleErrors().add(
                        RowError.ofFile(problem.with("sheet", sheet.getName()))));
                continue;
            }
            layouts.add(new Layout(sheet, type, properties));
        }
        return layouts;
    }

    /**
     * Adds a header column for every property the sheets map a column to, beyond the ones every
     * object has, and says where each lands.
     *
     * @return by property, the index of each of its columns in the header
     */
    private static Map<String, List<Integer>> extraColumns(List<Layout> layouts, List<String> header) {
        Map<String, Integer> occurrences = new LinkedHashMap<>();
        for (Layout layout : layouts) {
            Map<String, Integer> counts = new LinkedHashMap<>();
            layout.sheet.getMapping().values().stream().filter(ScientificObjectBulkImport::isExtra)
                    .forEach(target -> counts.merge(target, 1, Integer::sum));
            counts.forEach((target, count) -> occurrences.merge(target, count, Math::max));
        }
        Map<String, List<Integer>> positions = new LinkedHashMap<>();
        occurrences.forEach((target, count) -> {
            for (int i = 0; i < count; i++) {
                positions.computeIfAbsent(target, key -> new ArrayList<>()).add(header.size());
                header.add(target);
            }
        });
        return positions;
    }

    private static boolean isExtra(String target) {
        return !target.isEmpty() && !ObjectTargets.NAME.equals(target)
                && !ObjectTargets.GERMPLASM.equals(target) && !ObjectTargets.FACTOR_LEVEL.equals(target)
                && !ObjectTargets.isPosition(target);
    }

    /**
     * What a mapped cell is written as: as it is for a literal, the URI it names for a resource.
     * A parent must already exist in the experiment — the platform checks the objects of a file
     * against the instance before writing any of them, so a parent created by the same run would
     * be refused — and a row naming one is stopped with that explanation.
     */
    private String propertyValue(ObjectRow row, String column, String target, Layout layout, URI experiment,
                                 Map<String, String> sheetOfName, ObjectValueResolver resolver,
                                 GeneratedCsv csv) throws Exception {
        String value = row.cell(column);
        if (value == null) {
            return "";
        }
        if (ObjectTargets.PARENT.equals(target)) {
            ObjectValueResolver.Resolution existing = resolver.resolve(value, SCIENTIFIC_OBJECT, experiment);
            String sameRun = sheetOfName.get(value.toLowerCase(Locale.ROOT));
            if (!existing.isFound() && !existing.isAmbiguous() && sameRun != null) {
                csv.getModuleErrors().add(new RowError(row.getSheet(), row.getRow(), column, value,
                        RowError.Kind.UNRESOLVED, ReportMessage.of(ObjectSheets.PROBLEM + "parentInSameRun",
                                "'" + value + "' is created by sheet '" + sameRun + "' in this same "
                                        + "creation: create that sheet first, then this one.")
                        .with("value", value)
                        .with("sheet", sameRun)));
                return "";
            }
            return named(row, column, value, existing, "parent", csv);
        }
        TypeProperty property = layout.property(target);
        if (property != null && property.object()) {
            return named(row, column, value, resolver.resolve(value, property.range(), null), "value", csv);
        }
        return value;
    }

    private String named(ObjectRow row, String column, String value, ObjectValueResolver.Resolution found,
                         String what, GeneratedCsv csv) {
        if (found.isFound()) {
            return found.uri().toString();
        }
        String key = found.isAmbiguous() ? AMBIGUOUS + what : UNRESOLVED + what;
        String english = found.isAmbiguous()
                ? "Several resources are named '" + value + "'."
                : "Nothing is named '" + value + "'.";
        csv.getModuleErrors().add(new RowError(row.getSheet(), row.getRow(), column, value,
                RowError.Kind.UNRESOLVED, ReportMessage.of(key, english).with("value", value)));
        return "";
    }

    /**
     * The names of every row taking part, by lower case, with the sheet each belongs to.
     */
    private static Map<String, String> namesInThisRun(List<Layout> layouts) {
        Map<String, String> names = new HashMap<>();
        for (Layout layout : layouts) {
            for (ObjectRow row : layout.sheet.getRows()) {
                names.putIfAbsent(row.getName().toLowerCase(Locale.ROOT), layout.sheet.getName());
            }
        }
        return names;
    }

    private static URI absoluteUri(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(value.trim());
            return uri.isAbsolute() ? uri : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * A sheet taking part in the creation, with its type and what that type accepts.
     */
    private static final class Layout {

        private final ObjectSheet sheet;
        private final URI type;
        private final Map<String, TypeProperty> properties = new HashMap<>();

        private Layout(ObjectSheet sheet, URI type, List<TypeProperty> properties) {
            this.sheet = sheet;
            this.type = type;
            properties.forEach(property -> this.properties.put(property.uri().toString(), property));
        }

        private TypeProperty property(String target) {
            return properties.get(target);
        }

        /**
         * The cell of the column mapped to a target, when one is; otherwise what the profile made
         * of the row.
         */
        private String value(ObjectRow row, String target, String fallback) {
            List<String> columns = sheet.columnsMappedTo(target);
            return columns.isEmpty() ? fallback : row.cell(columns.get(0));
        }

        private boolean hasPositions() {
            return sheet.getRows().stream().anyMatch(row ->
                    value(row, ObjectTargets.X, row.getX()) != null || value(row, ObjectTargets.Y, row.getY()) != null);
        }
    }

    /**
     * A value the row names must resolve; a value it leaves empty stays empty.
     */
    private String resolve(ObjectRow row, String name, Map<String, URI> known, String what,
                           GeneratedCsv csv) {
        if (name == null) {
            return "";
        }
        URI uri = known.get(name.toLowerCase());
        if (uri == null) {
            csv.getModuleErrors().add(new RowError(row.getSheet(), row.getRow(), what, name,
                    RowError.Kind.UNRESOLVED,
                    ReportMessage.of(UNRESOLVED + what, "No " + what + " named '" + name + "'.")
                            .with("value", name)));
            return "";
        }
        return uri.toString();
    }

    /**
     * The experiment's factor levels by name. Only these may be used: the importer refuses any
     * other, and a treatment code of the workbook means one of them or nothing.
     */
    protected Map<String, URI> factorLevels(URI experiment) throws Exception {
        Map<String, URI> byName = new HashMap<>();
        for (FactorModel factor : new FactorDAO(sparql).getByExperiment(experiment, user.getLanguage())) {
            if (factor.getFactorLevels() == null) {
                continue;
            }
            for (FactorLevelModel level : factor.getFactorLevels()) {
                if (level.getName() != null) {
                    byName.put(level.getName().toLowerCase(), level.getUri());
                }
            }
        }
        return byName;
    }

    /**
     * What the types accept, as the platform's importer will judge it.
     */
    protected TypeProperties typeProperties() {
        return new TypeProperties(user.getLanguage());
    }

    /**
     * Where the names of parents and of other resources are looked up.
     */
    protected ObjectValueResolver valueResolver() {
        return new ObjectValueResolver(sparql);
    }

    protected String experimentStart(URI experiment) throws Exception {
        ExperimentModel model = new ExperimentDAO(sparql, nosql, fs).get(experiment, user);
        return model == null || model.getStartDate() == null ? "" : model.getStartDate().toString();
    }

    @Override
    protected CSVValidationModel validateWithPlatform(GeneratedCsv csv, Map<String, String> values)
            throws Exception {
        URI experiment = URI.create(values.get(EXPERIMENT));
        return withFile(csv, file -> new ScientificObjectCsvImporterLogic(
                sparql, nosql, experiment, user, fs, null).importCSV(file, true));
    }

    @Override
    protected List<RowError> errorsOf(CSVValidationModel validation, GeneratedCsv csv) {
        return PlatformValidationAdapter.fromCsvImporter(validation, csv.getOrigins());
    }

    /**
     * The same call the platform's import endpoint makes, in the same kind of transaction: the
     * objects and their moves are written together or not at all.
     */
    @Override
    protected BulkOutcome importWithPlatform(GeneratedCsv csv, Map<String, String> values)
            throws Exception {
        URI experiment = URI.create(values.get(EXPERIMENT));
        CSVValidationModel result = withFile(csv, file ->
                new SparqlMongoTransaction(sparql, nosql.getServiceV2()).execute(session ->
                        new ScientificObjectCsvImporterLogic(sparql, nosql, experiment, user, fs, session)
                                .importCSV(file, false)));
        List<RowError> errors = errorsOf(result, csv);
        return errors.isEmpty()
                ? BulkOutcome.imported(csv.getDataLineCount(), result.getNbObjectImported())
                : BulkOutcome.refused(csv.getDataLineCount(), errors);
    }

    /**
     * The importer reads a file, not a stream: the CSV is written to a temporary file that is
     * removed whatever happens.
     */
    private <T> T withFile(GeneratedCsv csv, ThrowingFunction<File, T> use) throws Exception {
        Path path = Files.createTempFile("ai-import-objects-", ".csv");
        try {
            Files.write(path, csv.render().getBytes(StandardCharsets.UTF_8));
            return use.apply(path.toFile());
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @FunctionalInterface
    private interface ThrowingFunction<A, B> {
        B apply(A value) throws Exception;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
