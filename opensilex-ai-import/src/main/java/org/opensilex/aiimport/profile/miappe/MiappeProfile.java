//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.miappe;

import org.opensilex.aiimport.mapping.ColumnRole;
import org.opensilex.aiimport.profile.DataPoint;
import org.opensilex.aiimport.profile.EventCandidate;
import org.opensilex.aiimport.profile.ExtractedImportPlan;
import org.opensilex.aiimport.profile.ImportProfile;
import org.opensilex.aiimport.profile.ObjectRow;
import org.opensilex.aiimport.profile.ObjectSheetDefaults;
import org.opensilex.aiimport.profile.ObjectTargets;
import org.opensilex.aiimport.profile.PersonCandidate;
import org.opensilex.aiimport.profile.VariableCandidate;
import org.opensilex.aiimport.profile.VariableComponent;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Profile for MIAPPE v1.1, the minimum information checklist for a plant phenotyping experiment.
 * <p>
 * MIAPPE is not a data entry template but a metadata checklist, and that changes what this profile
 * is for. The observations live in a separate file, referenced from the Data file section; what
 * this workbook carries is everything around them — the investigation, the study, who ran it, the
 * plant material, the observation units, the events, and the definition of each observed variable.
 * That is precisely the part an import usually has to invent, so a MIAPPE submission is the best
 * starting point of the three templates supported here.
 * <p>
 * Two of its conventions do real work. The trailing asterisk marks a mandatory field, so the
 * checklist itself says what a valid submission must carry and the report can name what is
 * missing without a rule being written here. And the Observed Variable section gives the trait,
 * the method and the scale, each with an optional ontology accession — the components an OpenSILEX
 * variable is made of, which no other supported template supplies.
 *
 * @author Arnaud Charleroy
 */
public class MiappeProfile implements ImportProfile {

    public static final String ID = "miappe";

    private static final String ANOMALY = "AiImport.report.anomaly.miappe.";

    //#region the fields this profile reads

    static final String INVESTIGATION_TITLE = "Investigation title";
    static final String INVESTIGATION_DESCRIPTION = "Investigation description";
    static final String MIAPPE_VERSION = "MIAPPE version";

    static final String STUDY_ID = "Study unique ID";
    static final String STUDY_TITLE = "Study title";
    static final String STUDY_DESCRIPTION = "Study description";
    static final String STUDY_START = "Start date of study";
    static final String STUDY_END = "End date of study";
    static final String STUDY_SITE = "Experimental site name";
    static final String STUDY_DESIGN = "Description of the experimental design";

    static final String PERSON_NAME = "Person name";
    static final String PERSON_EMAIL = "Person email";
    static final String PERSON_ID = "Person ID";
    static final String PERSON_ROLE = "Person role";
    static final String PERSON_AFFILIATION = "Person affiliation";

    static final String MATERIAL_ID = "Biological material ID";
    static final String MATERIAL_GENUS = "Genus";
    static final String MATERIAL_SPECIES = "Species";
    static final String MATERIAL_INFRASPECIFIC = "Infraspecific name";

    static final String OBSERVATION_UNIT_ID = "Observation unit ID";
    static final String OBSERVATION_UNIT_TYPE = "Observation unit type";
    static final String OBSERVATION_UNIT_MATERIAL = "Biological Material ID";
    static final String OBSERVATION_UNIT_FACTOR = "Observation Unit factor value";

    static final String VARIABLE_ID = "Variable ID";
    static final String VARIABLE_NAME = "Variable name";
    static final String VARIABLE_ACCESSION = "Variable accession number";
    static final String TRAIT = "Trait";
    static final String TRAIT_ACCESSION = "Trait accession number";
    static final String METHOD = "Method";
    static final String METHOD_ACCESSION = "Method accession number";
    static final String SCALE = "Scale";
    static final String SCALE_ACCESSION = "Scale accession number";

    static final String EVENT_TYPE = "Event type";
    static final String EVENT_DESCRIPTION = "Event description";
    static final String EVENT_DATE = "Event date";

    static final String FACTOR_TYPE = "Experimental Factor type";
    static final String FACTOR_VALUES = "Experimental Factor values";

    static final String DATA_FILE_LINK = "Data file link";

    //#endregion

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getLabel() {
        return "MIAPPE v1.1 plant phenotyping checklist";
    }

    /**
     * Recognised on the sections it declares, and confirmed by the version field.
     * <p>
     * A workbook can have a sheet called Study or Event by coincidence; it does not have eight of
     * the checklist's eleven sections by coincidence.
     */
    @Override
    public int match(WorkbookStructure structure) {
        MiappeSheets sheets = new MiappeSheets(structure);
        int sections = sheets.sectionCount();
        boolean declaresVersion = sheets.section(MiappeSheets.INVESTIGATION)
                .map(section -> !section.value(MIAPPE_VERSION).isEmpty()
                        || section.fields().contains(MIAPPE_VERSION))
                .orElse(false);

        if (sections >= 8 && declaresVersion) {
            return 100;
        }
        if (sections >= 8) {
            return 80;
        }
        if (sections >= 5) {
            return 40;
        }
        return 0;
    }

    @Override
    public String getPromptContext(WorkbookStructure structure) {
        MiappeSheets sheets = new MiappeSheets(structure);
        List<String> lines = new ArrayList<>(Arrays.asList(
                "This workbook follows MIAPPE v1.1, the minimum information checklist for a plant",
                "phenotyping experiment. It is a metadata checklist rather than a data entry file,",
                "which shapes what can be done with it:",
                "",
                "- The observations are NOT in this workbook. The 'Data file' section links to the",
                "  file that holds them. Do not offer to insert observation data from this file;",
                "  offer to prepare everything the data will need.",
                "- 'Investigation' describes the research programme and becomes a project.",
                "- 'Study' describes one experiment and becomes an experiment. Its 'Experimental",
                "  site name' becomes a facility.",
                "- 'Observation Unit' holds the plots, plants or pots, and becomes the scientific",
                "  objects. Their type is given by 'Observation unit type'.",
                "- 'Biological Material' becomes germplasm.",
                "- 'Observed Variable' defines each variable as a trait, a method and a scale, each",
                "  with an optional ontology accession. This is what OpenSILEX needs to create a",
                "  variable, except that OpenSILEX splits the trait into an entity and a",
                "  characteristic — 'plant height' becomes the entity 'plant' and the",
                "  characteristic 'height'. Propose that split, do not apply it silently.",
                "- 'Event' records what happened, with an optional observation unit it concerned.",
                "- 'Person', 'Environment', 'Exp. Factor', 'Sample' are read but have no direct",
                "  counterpart to create from this page.",
                "",
                "A field name ending in an asterisk is mandatory in the checklist. The three rows",
                "under each header hold the definition, an example and the format of each field:",
                "they are documentation, never values.",
                ""));

        String version = sheets.section(MiappeSheets.INVESTIGATION)
                .map(section -> section.value(MIAPPE_VERSION))
                .orElse("");
        if (!version.isEmpty()) {
            lines.add("The submission declares MIAPPE version " + version + ".");
        }
        return String.join("\n", lines);
    }

    @Override
    public ExtractedImportPlan extract(WorkbookStructure structure) {
        MiappeSheets sheets = new MiappeSheets(structure);
        ExtractedImportPlan plan = new ExtractedImportPlan().setProfileId(ID);

        readInvestigation(sheets, plan);
        readStudies(sheets, plan);
        readPeople(sheets, plan);
        readMaterial(sheets, plan);
        readObservationUnits(sheets, plan);
        readVariables(sheets, plan);
        readFactors(sheets, plan);
        readDataFiles(sheets, plan);
        checkMandatoryFields(sheets, plan);

        return plan;
    }

    //#region sections

    private void readInvestigation(MiappeSheets sheets, ExtractedImportPlan plan) {
        sheets.section(MiappeSheets.INVESTIGATION).ifPresent(section -> {
            String title = section.value(INVESTIGATION_TITLE);
            if (!title.isEmpty()) {
                plan.getProjectNames().add(title);
            }
            note(plan, "investigation.description", section.value(INVESTIGATION_DESCRIPTION));
            note(plan, "miappe.version", section.value(MIAPPE_VERSION));
        });
    }

    private void readStudies(MiappeSheets sheets, ExtractedImportPlan plan) {
        sheets.section(MiappeSheets.STUDY).ifPresent(section -> {
            for (List<String> row : section.values()) {
                String title = section.cell(row, STUDY_TITLE);
                String id = section.cell(row, STUDY_ID);
                String name = !title.isEmpty() ? title : id;
                if (!name.isEmpty()) {
                    plan.getExperimentNames().add(name);
                }
                String site = section.cell(row, STUDY_SITE);
                if (!site.isEmpty() && !plan.getFacilityNames().contains(site)) {
                    plan.getFacilityNames().add(site);
                }
                note(plan, "study.description", section.cell(row, STUDY_DESCRIPTION));
                note(plan, "study.design", section.cell(row, STUDY_DESIGN));
                note(plan, "study.startDate", section.cell(row, STUDY_START));
                note(plan, "study.endDate", section.cell(row, STUDY_END));
            }
        });
    }

    /**
     * The Person section becomes people, not observer codes.
     * <p>
     * MIAPPE describes a study's contact, its authors, whoever submitted the data — each with an
     * email, an ORCID and an affiliation. OpenSILEX has a Person for exactly that. Reading them as
     * observer names kept the name and threw the rest away.
     */
    private void readPeople(MiappeSheets sheets, ExtractedImportPlan plan) {
        sheets.section(MiappeSheets.PERSON).ifPresent(section -> {
            for (List<String> row : section.values()) {
                String name = section.cell(row, PERSON_NAME);
                String email = section.cell(row, PERSON_EMAIL);
                if (name.isEmpty() && email.isEmpty()) {
                    continue;
                }
                plan.getPersons().add(new PersonCandidate(name)
                        .setEmail(email)
                        .setOrcid(orcidOf(section.cell(row, PERSON_ID)))
                        .setRole(section.cell(row, PERSON_ROLE))
                        .setAffiliation(section.cell(row, PERSON_AFFILIATION)));
            }
        });
    }

    /**
     * The Person ID field takes any identifier and recommends an ORCID, so only an ORCID is read as
     * one — a laboratory's internal code stored as an ORCID would be a false identifier, and the
     * kind that is never noticed.
     * <p>
     * The field is semicolon-separated in the checklist; the first ORCID-shaped value wins.
     */
    private String orcidOf(String personId) {
        if (personId == null || personId.trim().isEmpty()) {
            return null;
        }
        for (String candidate : personId.split(";")) {
            String trimmed = candidate.trim();
            if (trimmed.toLowerCase(Locale.ROOT).contains("orcid.org/")) {
                return trimmed.startsWith("http") ? trimmed : "https://" + trimmed;
            }
        }
        return null;
    }

    private void readMaterial(MiappeSheets sheets, ExtractedImportPlan plan) {
        sheets.section(MiappeSheets.BIOLOGICAL_MATERIAL).ifPresent(section -> {
            for (List<String> row : section.values()) {
                // The checklist's own identifier first; the botanical name is what a germplasm is
                // usually registered under, so it is offered as a fallback.
                String id = section.cell(row, MATERIAL_ID);
                String botanical = botanicalName(section, row);
                String name = !id.isEmpty() ? id : botanical;
                if (!name.isEmpty() && !plan.getGermplasmNames().contains(name)) {
                    plan.getGermplasmNames().add(name);
                }
                note(plan, "material.botanicalName", botanical);
            }
        });
    }

    private String botanicalName(MiappeSection section, List<String> row) {
        StringBuilder name = new StringBuilder();
        for (String field : Arrays.asList(MATERIAL_GENUS, MATERIAL_SPECIES, MATERIAL_INFRASPECIFIC)) {
            String part = section.cell(row, field);
            if (!part.isEmpty()) {
                if (name.length() > 0) {
                    name.append(' ');
                }
                name.append(part);
            }
        }
        return name.toString();
    }

    private void readObservationUnits(MiappeSheets sheets, ExtractedImportPlan plan) {
        sheets.section(MiappeSheets.OBSERVATION_UNIT).ifPresent(section -> {
            plan.getScientificObjectNames().addAll(section.distinct(OBSERVATION_UNIT_ID));
            List<String> types = section.distinct(OBSERVATION_UNIT_TYPE);
            if (!types.isEmpty()) {
                note(plan, "observationUnit.types", String.join(", ", types));
            }
        });
    }

    /**
     * Reads the variable definitions, components included.
     * <p>
     * The trait, method and scale are carried as they stand. The trait is not split into an entity
     * and a characteristic here: that split is a judgement about the user's science, and it belongs
     * in a proposal they can correct, not in a reader.
     */
    private void readVariables(MiappeSheets sheets, ExtractedImportPlan plan) {
        sheets.section(MiappeSheets.OBSERVED_VARIABLE).ifPresent(section -> {
            for (List<String> row : section.values()) {
                String id = section.cell(row, VARIABLE_ID);
                String name = section.cell(row, VARIABLE_NAME);
                if (id.isEmpty() && name.isEmpty()) {
                    continue;
                }
                VariableCandidate candidate = new VariableCandidate(!id.isEmpty() ? id : name)
                        .setLabel(name)
                        .setExternalId(section.cell(row, VARIABLE_ACCESSION))
                        .setTrait(new VariableComponent(
                                section.cell(row, TRAIT), section.cell(row, TRAIT_ACCESSION)))
                        .setMethod(new VariableComponent(
                                section.cell(row, METHOD), section.cell(row, METHOD_ACCESSION)))
                        .setUnit(new VariableComponent(
                                section.cell(row, SCALE), section.cell(row, SCALE_ACCESSION)));
                candidate.addSheet(section.getName());
                plan.getVariables().add(candidate);
            }
        });
    }

    private void readFactors(MiappeSheets sheets, ExtractedImportPlan plan) {
        sheets.section(MiappeSheets.EXPERIMENTAL_FACTOR).ifPresent(section -> {
            List<String> factors = new ArrayList<>();
            for (List<String> row : section.values()) {
                String type = section.cell(row, FACTOR_TYPE);
                if (type.isEmpty()) {
                    continue;
                }
                String values = section.cell(row, FACTOR_VALUES);
                factors.add(values.isEmpty() ? type : type + " (" + values + ")");
            }
            if (!factors.isEmpty()) {
                note(plan, "experimentalFactors", String.join("; ", factors));
            }
        });
    }

    /**
     * The observations are elsewhere, and saying so plainly saves the user looking for them.
     */
    private void readDataFiles(MiappeSheets sheets, ExtractedImportPlan plan) {
        sheets.section(MiappeSheets.DATA_FILE).ifPresent(section -> {
            List<String> links = section.distinct(DATA_FILE_LINK);
            if (!links.isEmpty()) {
                note(plan, "dataFiles", String.join("; ", links));
            }
        });
    }

    /**
     * Reports the mandatory fields the submission leaves empty, section by section.
     * <p>
     * This is the checklist doing its own work: the asterisks are MIAPPE's statement of what a
     * valid submission carries, so an empty one is a fact about the file, not an opinion about it.
     */
    private void checkMandatoryFields(MiappeSheets sheets, ExtractedImportPlan plan) {
        int emptySections = 0;
        for (String name : MiappeSheets.SECTIONS) {
            Optional<MiappeSection> found = sheets.section(name);
            if (!found.isPresent()) {
                continue;
            }
            MiappeSection section = found.get();
            List<String> unfilled = section.unfilledMandatoryFields();
            if (unfilled.isEmpty()) {
                continue;
            }
            if (unfilled.size() == section.mandatoryFields().size()) {
                emptySections++;
                continue;
            }
            plan.addAnomaly(ReportMessage.of(ANOMALY + "mandatoryFieldsEmpty",
                            "Section '" + name + "': the mandatory fields "
                                    + String.join(", ", unfilled) + " are empty.")
                    .with("section", name)
                    .with("fields", String.join(", ", unfilled)));
        }
        if (emptySections > 0) {
            plan.addAnomaly(ReportMessage.of(ANOMALY + "blankSections",
                            emptySections + " of the " + MiappeSheets.SECTIONS.size()
                                    + " MIAPPE sections are still blank templates, with no values "
                                    + "under the header.")
                    .with("count", emptySections)
                    .with("total", MiappeSheets.SECTIONS.size()));
        }
    }

    //#endregion

    /**
     * The events the submission records, each concerning the observation unit it names.
     * <p>
     * The event type is a Crop Ontology accession in MIAPPE and a class in OpenSILEX, but the
     * shipped event vocabulary covers device maintenance rather than agronomy, so the accession is
     * kept in the description instead of being mapped onto a class that would misstate it.
     */
    @Override
    public List<EventCandidate> extractEvents(WorkbookStructure structure) {
        List<EventCandidate> events = new ArrayList<>();
        new MiappeSheets(structure).section(MiappeSheets.EVENT).ifPresent(section -> {
            List<List<String>> rows = section.values();
            for (int i = 0; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                LocalDate date = isoDate(section.cell(row, EVENT_DATE));
                if (date == null) {
                    continue;
                }
                EventCandidate event = new EventCandidate()
                        .setSheet(section.getName())
                        .setRowNumber(i + 1)
                        .setDate(date)
                        .setTypeLabel(section.cell(row, EVENT_TYPE))
                        .setDescription(section.cell(row, EVENT_DESCRIPTION))
                        .setTargetKind(DataPoint.TargetKind.SCIENTIFIC_OBJECT);
                event.addTarget(section.cell(row, OBSERVATION_UNIT_ID));
                events.add(event);
            }
        });
        return events;
    }

    /**
     * MIAPPE dates are ISO 8601, optionally with a time and a zone. Only the day is kept, which is
     * what an event needs; a value that is not a date is left alone rather than coerced.
     */
    private LocalDate isoDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String text = value.trim();
        int separator = text.indexOf('T');
        if (separator > 0) {
            text = text.substring(0, separator);
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Column roles for the mapping panel. A MIAPPE section names its fields in prose, so the role
     * comes from the section rather than from the header.
     */
    @Override
    public ColumnRole roleOf(WorkbookStructure structure, String sheetName, String header) {
        String field = MiappeSection.bare(header);
        if (field.equalsIgnoreCase(OBSERVATION_UNIT_ID)) {
            return ColumnRole.OBJECT;
        }
        if (field.equalsIgnoreCase(STUDY_TITLE) || field.equalsIgnoreCase(STUDY_ID)) {
            return ColumnRole.TRIAL;
        }
        if (field.equalsIgnoreCase(MATERIAL_ID)) {
            return ColumnRole.GERMPLASM;
        }
        if (field.equalsIgnoreCase(STUDY_SITE)) {
            return ColumnRole.LOCATION;
        }
        if (field.equalsIgnoreCase(PERSON_NAME)) {
            return ColumnRole.PERSON;
        }
        if (field.toLowerCase(Locale.ROOT).endsWith("date")) {
            return ColumnRole.DATE;
        }
        // A MIAPPE workbook holds no measurements, so no column is ever a variable's values.
        return ColumnRole.UNKNOWN;
    }

    private void note(ExtractedImportPlan plan, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            plan.note(key, value.trim());
        }
    }

    /**
     * The observation units, with the material they hold and the factor value they receive.
     */
    @Override
    public List<ObjectRow> extractObjectRows(WorkbookStructure structure) {
        List<ObjectRow> rows = new ArrayList<>();
        new MiappeSheets(structure).section(MiappeSheets.OBSERVATION_UNIT).ifPresent(section -> {
            for (List<String> row : section.values()) {
                String name = section.cell(row, OBSERVATION_UNIT_ID);
                if (!name.isEmpty()) {
                    rows.add(new ObjectRow(section.getName(), section.rowNumberOf(row), name)
                            .setGermplasm(section.cell(row, OBSERVATION_UNIT_MATERIAL))
                            .setFactorLevel(section.cell(row, OBSERVATION_UNIT_FACTOR))
                            .setCells(section.fields(), row));
                }
            }
        });
        return rows;
    }

    /**
     * The observation units: their identifier, the material they hold, the factor value they
     * receive; the other fields start unmapped.
     */
    @Override
    public ObjectSheetDefaults objectSheetDefaults(WorkbookStructure structure, String sheetName) {
        boolean units = new MiappeSheets(structure).section(MiappeSheets.OBSERVATION_UNIT)
                .map(section -> section.getName().equals(sheetName))
                .orElse(false);
        if (!units) {
            return ObjectSheetDefaults.none();
        }
        Map<String, String> targets = new LinkedHashMap<>();
        targets.put(OBSERVATION_UNIT_ID, ObjectTargets.NAME);
        targets.put(OBSERVATION_UNIT_MATERIAL, ObjectTargets.GERMPLASM);
        targets.put(OBSERVATION_UNIT_FACTOR, ObjectTargets.FACTOR_LEVEL);
        return new ObjectSheetDefaults(OBSERVATION_UNIT_ID, targets, null);
    }

    /**
     * Never any observations: they live in the file the Data file section links to.
     */
    @Override
    public List<DataPoint> extractDataPoints(WorkbookStructure structure) {
        return new ArrayList<>();
    }
}
