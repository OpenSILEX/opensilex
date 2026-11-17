//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

import org.opensilex.aiimport.mapping.ColumnRole;
import org.opensilex.aiimport.profile.DataPoint;
import org.opensilex.aiimport.profile.EventCandidate;
import org.opensilex.aiimport.profile.ExtractedImportPlan;
import org.opensilex.aiimport.profile.FactorLevelCandidate;
import org.opensilex.aiimport.profile.ImportProfile;
import org.opensilex.aiimport.profile.ObjectRow;
import org.opensilex.aiimport.profile.ObjectSheetDefaults;
import org.opensilex.aiimport.profile.PersonCandidate;
import org.opensilex.aiimport.profile.VariableCandidate;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The profile of a file the user described themselves: which sheets hold objects, observations,
 * levels of a factor or facilities, and what each column is. It knows nothing of any template; what
 * it reads is what it was told.
 * <p>
 * A thin front: the work is done by {@link DescribedSheet} (what a sheet is), {@link ObjectMerger}
 * (the objects, across sheets) and {@link DataReader} (the observations), each of which can be
 * tested on its own. Never chosen by itself — it recognises no file — but bound to a conversation by
 * {@code SessionProfile}, so that it carries that conversation's description.
 * <p>
 * Like every profile it reads and never writes, and never looks anything up in the instance: the
 * names it extracts are resolved afterwards, as for any other file.
 *
 * @author Arnaud Charleroy
 */
public class UserDefinedProfile implements ImportProfile {

    public static final String ID = "custom";

    private static final String NO_EXPERIMENT = DescriptionRules.PREFIX + "noExperiment";

    private final FileDescription description;

    /**
     * The sheets as described, worked out once per workbook: a profile is asked about every column
     * of every sheet, and a default read from the values of a large sheet is not free.
     */
    private WorkbookStructure workedOutFor;
    private List<DescribedSheet> described;
    private ObjectMerger.Merged merged;
    private DataReader.Read read;
    private EventReader.Read eventsRead;

    public UserDefinedProfile() {
        this(new FileDescription());
    }

    public UserDefinedProfile(FileDescription description) {
        this.description = description;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getLabel() {
        return "File described by the user";
    }

    /**
     * Never recognises a file: the user chooses to describe one, and the profile is then bound to
     * their conversation.
     */
    @Override
    public int match(WorkbookStructure structure) {
        return 0;
    }

    @Override
    public String getPromptContext(WorkbookStructure structure) {
        return String.join("\n", Arrays.asList(
                "The user described this file themselves — which sheets hold objects, observations, levels",
                "of a factor or facilities, and what each column is. What they said is not a guess: use it,",
                "and do not suggest another mapping unless a value contradicts it.",
                "A sheet whose description is incomplete is not read, and the report says why: point the",
                "user to the description panel rather than describing the problem as a fault of the file."
        ));
    }

    @Override
    public ColumnRole roleOf(WorkbookStructure structure, String sheetName, String header) {
        if (header == null || header.isEmpty()) {
            return ColumnRole.UNKNOWN;
        }
        return sheet(structure, sheetName).map(sheet -> sheet.roleOf(header).columnRole())
                .orElse(ColumnRole.UNKNOWN);
    }

    @Override
    public ExtractedImportPlan extract(WorkbookStructure structure) {
        ExtractedImportPlan plan = new ExtractedImportPlan().setProfileId(ID);
        List<DescribedSheet> sheets = sheets(structure);

        boolean needsExperiment = false;
        boolean namesTheExperiment = false;
        Map<String, Set<String>> names = new LinkedHashMap<>();
        for (DescribedSheet sheet : sheets) {
            if (!sheet.kind().isRead()) {
                continue;
            }
            sheet.problems().forEach(plan::addAnomaly);
            if (!sheet.isReadable()) {
                continue;
            }
            needsExperiment |= ExperimentNaming.needs(sheet.kind());
            for (DescribedRole role : Arrays.asList(DescribedRole.TRIAL, DescribedRole.PROJECT,
                    DescribedRole.PERSON, DescribedRole.OBSERVER, DescribedRole.GERMPLASM, DescribedRole.OBJECT,
                    DescribedRole.LOCATION)) {
                for (String column : sheet.columns(role)) {
                    Set<String> values = new LinkedHashSet<>(sheet.sheet().distinctValues(column));
                    namesTheExperiment |= role == DescribedRole.TRIAL && !values.isEmpty();
                    names.computeIfAbsent(role.name(), key -> new LinkedHashSet<>()).addAll(values);
                }
            }
            for (String column : sheet.columns(DescribedRole.VARIABLE)) {
                candidate(plan, column).addSheet(sheet.name());
            }
        }

        plan.getExperimentDetails().putAll(FieldValues.detailsOf(sheets, FormTarget.Entity.EXPERIMENT));
        plan.getProjectDetails().putAll(FieldValues.detailsOf(sheets, FormTarget.Entity.PROJECT));
        FieldValues.commentsOf(sheets, SheetKind.FACILITIES, DescribedRole.LOCATION).forEach((facility, text) ->
                plan.getFacilityDetails().computeIfAbsent(facility, key -> new LinkedHashMap<>())
                        .put("description", text));
        plan.getExperimentNames().addAll(names.getOrDefault(DescribedRole.TRIAL.name(), Set.of()));
        plan.getProjectNames().addAll(names.getOrDefault(DescribedRole.PROJECT.name(), Set.of()));
        plan.getObserverNames().addAll(names.getOrDefault(DescribedRole.OBSERVER.name(), Set.of()));
        plan.getGermplasmNames().addAll(names.getOrDefault(DescribedRole.GERMPLASM.name(), Set.of()));
        plan.getScientificObjectNames().addAll(names.getOrDefault(DescribedRole.OBJECT.name(), Set.of()));
        plan.getFacilityNames().addAll(names.getOrDefault(DescribedRole.LOCATION.name(), Set.of()));
        names.getOrDefault(DescribedRole.PERSON.name(), Set.of())
                .forEach(person -> plan.getPersons().add(new PersonCandidate(person)));

        if (!namesTheExperiment && description.getExperimentName() != null) {
            plan.getExperimentNames().add(description.getExperimentName());
        } else if (!namesTheExperiment && needsExperiment) {
            plan.addAnomaly(ReportMessage.of(NO_EXPERIMENT, "No column names the experiment, and none was "
                    + "chosen: objects, levels and observations cannot be checked until one is."));
        }

        merged(structure).anomalies().forEach(plan::addAnomaly);
        read(structure).anomalies().forEach(plan::addAnomaly);
        events(structure).anomalies().forEach(plan::addAnomaly);
        plan.note("mapping", "described by the user");
        sheets.stream().filter(sheet -> sheet.sheet().getCharset() != null).forEach(sheet ->
                plan.note("charset of " + sheet.name(), sheet.sheet().getCharset() + ": not UTF-8, so an accent "
                        + "may be another letter"));
        return plan;
    }

    @Override
    public List<ObjectRow> extractObjectRows(WorkbookStructure structure) {
        return merged(structure).rows();
    }

    @Override
    public ObjectSheetDefaults objectSheetDefaults(WorkbookStructure structure, String sheetName) {
        Map<String, String> targets = merged(structure).targetsBySheet().get(sheetName);
        Optional<DescribedSheet> sheet = sheet(structure, sheetName);
        if (targets == null || !sheet.isPresent()) {
            return ObjectSheetDefaults.none();
        }
        URI type = sheet.get().description() == null ? null : sheet.get().description().getType();
        // Never a type the user did not choose: nothing in a file says what its objects are.
        return new ObjectSheetDefaults(sheet.get().column(DescribedRole.OBJECT), targets, type);
    }

    /**
     * The levels of the factors the file declares: the columns described as levels of the sheets of
     * objects — whose header is the factor — and the sheets of levels themselves.
     */
    @Override
    public List<FactorLevelCandidate> extractFactorLevels(WorkbookStructure structure) {
        List<FactorLevelCandidate> levels = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (DescribedSheet sheet : sheets(structure)) {
            if (!sheet.isReadable()) {
                continue;
            }
            SheetStructure data = sheet.sheet();
            if (sheet.kind() == SheetKind.FACTORS) {
                String factorColumn = sheet.column(DescribedRole.FACTOR);
                String levelColumn = sheet.column(DescribedRole.FACTOR_LEVEL);
                String levelNameColumn = sheet.column(DescribedRole.FACTOR_LEVEL_NAME);
                List<String> descriptionColumns = new ArrayList<>(sheet.columns(DescribedRole.FACTOR_LEVEL_DESCRIPTION));
                descriptionColumns.addAll(sheet.columns(DescribedRole.COMMENT));
                List<String> factorDescriptionColumns = sheet.columns(DescribedRole.FACTOR_DESCRIPTION);
                // The factor the user named stands for the column that would say it.
                String sheetFactor = sheet.description() == null ? null : sheet.description().getFactor();
                for (int i = 0; i < data.getRows().size(); i++) {
                    List<String> row = data.getRows().get(i);
                    add(levels, seen, sheet.name(), i + 2,
                            factorColumn == null ? sheetFactor : data.cell(row, factorColumn),
                            data.cell(row, levelColumn),
                            levelNameColumn == null ? null : data.cell(row, levelNameColumn),
                            FieldValues.joined(data, row, descriptionColumns),
                            FieldValues.joined(data, row, factorDescriptionColumns));
                }
            } else if (sheet.kind().describesObjects()) {
                for (String column : sheet.columns(DescribedRole.FACTOR_LEVEL)) {
                    for (int i = 0; i < data.getRows().size(); i++) {
                        add(levels, seen, sheet.name(), i + 2, column,
                                data.cell(data.getRows().get(i), column), null, null, null);
                    }
                }
            }
        }
        return levels;
    }

    @Override
    public List<DataPoint> extractDataPoints(WorkbookStructure structure) {
        return read(structure).points();
    }

    private static void add(List<FactorLevelCandidate> levels, Set<String> seen, String sheet, int row,
                            String factor, String level, String levelName, String comment, String factorComment) {
        String code = level == null ? "" : level.trim();
        String name = factor == null ? "" : factor.trim();
        if (code.isEmpty() || name.isEmpty() || !seen.add((name + "|" + code).toLowerCase(Locale.ROOT))) {
            return;
        }
        levels.add(new FactorLevelCandidate(sheet, row, name, code, blankToNull(levelName), blankToNull(comment),
                blankToNull(factorComment)));
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private static VariableCandidate candidate(ExtractedImportPlan plan, String columnKey) {
        for (VariableCandidate existing : plan.getVariables()) {
            if (existing.getColumnKey().equalsIgnoreCase(columnKey)) {
                return existing;
            }
        }
        VariableCandidate created = new VariableCandidate(columnKey);
        plan.getVariables().add(created);
        return created;
    }

    private Optional<DescribedSheet> sheet(WorkbookStructure structure, String name) {
        return sheets(structure).stream().filter(sheet -> sheet.name().equals(name)).findFirst();
    }

    private List<DescribedSheet> sheets(WorkbookStructure structure) {
        if (workedOutFor != structure || described == null) {
            List<DescribedSheet> sheets = new ArrayList<>();
            for (SheetStructure sheet : structure.getSheets()) {
                sheets.add(DescribedSheet.of(sheet, description.find(sheet.getName()).orElse(null)));
            }
            described = sheets;
            merged = null;
            read = null;
            eventsRead = null;
            workedOutFor = structure;
        }
        return described;
    }

    private ObjectMerger.Merged merged(WorkbookStructure structure) {
        List<DescribedSheet> sheets = sheets(structure);
        if (merged == null) {
            merged = ObjectMerger.merge(sheets);
        }
        return merged;
    }

    private EventReader.Read events(WorkbookStructure structure) {
        List<DescribedSheet> sheets = sheets(structure);
        if (eventsRead == null) {
            eventsRead = EventReader.read(sheets);
        }
        return eventsRead;
    }

    /**
     * The events of the sheets described as events, each of the type the user chose for its sheet.
     */
    @Override
    public List<EventCandidate> extractEvents(WorkbookStructure structure) {
        return events(structure).events();
    }

    private DataReader.Read read(WorkbookStructure structure) {
        List<DescribedSheet> sheets = sheets(structure);
        if (read == null) {
            read = DataReader.read(sheets);
        }
        return read;
    }
}
