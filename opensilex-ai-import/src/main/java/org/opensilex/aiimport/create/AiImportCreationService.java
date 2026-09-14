//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create;

import org.opensilex.aiimport.mapping.ColumnMapping;
import org.opensilex.aiimport.mapping.ColumnRole;
import org.opensilex.aiimport.profile.DataPoint;
import org.opensilex.aiimport.profile.EventCandidate;
import org.opensilex.aiimport.profile.star.StarProfile;
import org.opensilex.aiimport.resolve.ResolutionReport;
import org.opensilex.aiimport.resolve.ResolutionStatus;
import org.opensilex.aiimport.resolve.ResolvedItem;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.core.CoreModule;
import org.opensilex.core.data.api.DataCreationDTO;
import org.opensilex.core.data.bll.DataLogic;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.data.dal.DataProvenanceModel;
import org.opensilex.core.event.bll.EventLogic;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.project.dal.ProjectDAO;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.variable.bll.VariableCopyLogic;
import org.opensilex.core.variable.bll.VariableCopyResult;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.model.time.InstantModel;
import org.opensilex.sparql.service.SPARQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Creates what the report said was missing: a project, an experiment, or the observations
 * themselves.
 * <p>
 * Everything is checked before anything is written. The rules live here rather than in the API so
 * that the same computation answers both questions the interface asks — "what would this take?"
 * before the user fills the form, and "is this allowed?" when they submit it.
 *
 * @author Arnaud Charleroy
 */
public class AiImportCreationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiImportCreationService.class);

    /**
     * Matches the limit the data API enforces on a single call, so a large workbook is refused with
     * an explanation rather than halfway through.
     */
    public static final int MAX_DATA_POINTS = 50_000;

    /**
     * How many offending rows a refusal quotes. Enough to see the pattern; a file where every row
     * fails does not need every row listed.
     */
    private static final int MAX_UNRESOLVED_REPORTED = 20;

    private final SPARQLService sparql;
    private final MongoDBService nosql;
    private final FileStorageService fs;
    private final AccountModel currentUser;

    /**
     * Null outside the API, where nothing imports from a shared resource instance.
     */
    private final CoreModule coreModule;

    public AiImportCreationService(SPARQLService sparql, MongoDBService nosql, FileStorageService fs,
                                   AccountModel currentUser) {
        this(sparql, nosql, fs, currentUser, null);
    }

    /**
     * @param coreModule needed only to import a variable from a shared resource instance, which is
     *                   where the declared instances are configured
     */
    public AiImportCreationService(SPARQLService sparql, MongoDBService nosql, FileStorageService fs,
                                   AccountModel currentUser, CoreModule coreModule) {
        this.sparql = sparql;
        this.nosql = nosql;
        this.fs = fs;
        this.currentUser = currentUser;
        this.coreModule = coreModule;
    }

    /**
     * What this conversation already knows. Built per call rather than held: a session is answered
     * by a request-scoped service, and a cached view of a report that revalidation replaces would
     * answer yesterday's question.
     */
    private SessionFacts facts(AiImportSession session) {
        return new SessionFacts(session);
    }

    //#region what each creation would take

    public CreationRequirements requirementsFor(CreationTarget target, AiImportSession session) {
        switch (target) {
            case PROJECT:
                return projectRequirements(session);
            case EXPERIMENT:
                return experimentRequirements(session);
            case VARIABLE:
                return variableRequirements(session);
            case EVENT:
                return eventRequirements(session);
            case DATA:
            default:
                return dataRequirements(session);
        }
    }

    private CreationRequirements projectRequirements(AiImportSession session) {
        CreationRequirements requirements = new CreationRequirements(CreationTarget.PROJECT);

        RequiredField name = new RequiredField("name", "AiImport.proposal.field.name", "text", true);
        facts(session).firstMissing(session.getReport() == null ? null : session.getReport().getProjects())
                .ifPresent(value -> name.suggest(value, "AiImport.proposal.from.projectColumn"));
        requirements.field(name);

        requirements.field(new RequiredField("shortname", "AiImport.proposal.field.shortname", "text", false));

        RequiredField startDate = new RequiredField("start_date", "AiImport.proposal.field.startDate",
                "date", true);
        facts(session).earliestObservation().ifPresent(date ->
                startDate.suggest(date.toString(), "AiImport.proposal.from.earliestObservation"));
        requirements.field(startDate);

        RequiredField endDate = new RequiredField("end_date", "AiImport.proposal.field.endDate",
                "date", false);
        facts(session).latestObservation().ifPresent(date ->
                endDate.suggest(date.toString(), "AiImport.proposal.from.latestObservation"));
        requirements.field(endDate);

        requirements.field(new RequiredField("objective", "AiImport.proposal.field.objective",
                RequiredField.KIND_LONG_TEXT, false));
        requirements.field(new RequiredField("description", "AiImport.proposal.field.description",
                RequiredField.KIND_LONG_TEXT, false));

        return requirements;
    }

    private CreationRequirements experimentRequirements(AiImportSession session) {
        CreationRequirements requirements = new CreationRequirements(CreationTarget.EXPERIMENT);

        RequiredField name = new RequiredField("name", "AiImport.proposal.field.name", "text", true);
        facts(session).firstMissing(session.getReport() == null ? null : session.getReport().getExperiments())
                .ifPresent(value -> name.suggest(value, "AiImport.proposal.from.trialColumn"));
        requirements.field(name);

        // Required by the experiment model, and nothing in a data entry file says what it was.
        requirements.field(new RequiredField("objective", "AiImport.proposal.field.objective",
                RequiredField.KIND_LONG_TEXT, true));

        RequiredField startDate = new RequiredField("start_date", "AiImport.proposal.field.startDate",
                "date", true);
        facts(session).earliestObservation().ifPresent(date ->
                startDate.suggest(date.toString(), "AiImport.proposal.from.earliestObservation"));
        requirements.field(startDate);

        RequiredField endDate = new RequiredField("end_date", "AiImport.proposal.field.endDate",
                "date", false);
        facts(session).latestObservation().ifPresent(date ->
                endDate.suggest(date.toString(), "AiImport.proposal.from.latestObservation"));
        requirements.field(endDate);

        requirements.field(new RequiredField("projects", "AiImport.proposal.field.projects",
                RequiredField.KIND_URI_LIST, false)
                .setResource(RequiredField.RESOURCE_PROJECT));
        requirements.field(new RequiredField("description", "AiImport.proposal.field.description",
                RequiredField.KIND_LONG_TEXT, false));

        if (session.getReport() != null) {
            for (ResolvedItem project : session.getReport().getProjects()) {
                if (project.getStatus() == ResolutionStatus.MISSING) {
                    requirements.warn("AiImport.proposal.warn.projectMissing");
                }
            }
        }
        return requirements;
    }

    /**
     * What importing the variables takes.
     * <p>
     * Nothing the user has to type: the report already knows which variables exist on a shared
     * resource instance and where. Importing brings the variable and its four components in one
     * transaction, which is the cheapest way to a resolved variable and the one to offer first —
     * a component created here by hand is a permanent entry in this instance's referential.
     */
    private CreationRequirements variableRequirements(AiImportSession session) {
        CreationRequirements requirements = new CreationRequirements(CreationTarget.VARIABLE);

        ResolutionReport report = session.getReport();
        if (report == null) {
            requirements.block("AiImport.proposal.block.notAnalysed");
            return requirements;
        }

        Map<URI, List<URI>> byInstance = facts(session).importableVariables();
        if (byInstance.isEmpty()) {
            requirements.block("AiImport.proposal.block.noImportableVariable");
            return requirements;
        }

        int count = byInstance.values().stream().mapToInt(List::size).sum();
        requirements.field(new RequiredField("variable_summary",
                "AiImport.proposal.field.variableSummary", "text", false)
                .suggest(count + " — " + facts(session).describeInstances(byInstance),
                        "AiImport.proposal.from.sharedResources"));

        long stillMissing = facts(session).countStatus(report.getVariables(), ResolutionStatus.MISSING);
        if (stillMissing > 0) {
            requirements.warn("AiImport.proposal.warn.variablesStillMissing");
        }
        return requirements;
    }



    /**
     * What creating the events takes.
     * <p>
     * Little, deliberately: an event carries a date, a description and what it concerned, and the
     * file supplies all three. The one thing that has to exist first is what they concerned, since
     * an event with no target is refused by the model.
     */
    private CreationRequirements eventRequirements(AiImportSession session) {
        CreationRequirements requirements = new CreationRequirements(CreationTarget.EVENT);

        List<EventCandidate> events = session.getEvents();
        if (events.isEmpty()) {
            requirements.block("AiImport.proposal.block.noEvents");
            return requirements;
        }
        ResolutionReport report = session.getReport();
        if (report == null) {
            requirements.block("AiImport.proposal.block.notAnalysed");
            return requirements;
        }

        // Named rather than counted: the user reads how many events, of what, over what period.
        requirements.field(new RequiredField("event_summary", "AiImport.proposal.field.eventSummary",
                "text", false).suggest(facts(session).summarise(events), "AiImport.proposal.from.eventSheets"));

        long withoutTarget = events.stream()
                .filter(event -> facts(session).resolveTargets(event).isEmpty())
                .count();
        if (withoutTarget == events.size()) {
            requirements.block("AiImport.proposal.block.eventTargetsMissing");
        } else if (withoutTarget > 0) {
            requirements.warn("AiImport.proposal.warn.someEventTargetsMissing");
        }
        return requirements;
    }



    /**
     * Data is the one creation with real prerequisites: every value needs an object to sit on and a
     * variable to be an instance of, and neither can be invented here.
     */
    private CreationRequirements dataRequirements(AiImportSession session) {
        CreationRequirements requirements = new CreationRequirements(CreationTarget.DATA);

        RequiredField experiment = new RequiredField("experiment", "AiImport.proposal.field.experiment",
                RequiredField.KIND_URI, true)
                .setResource(RequiredField.RESOURCE_EXPERIMENT);
        facts(session).firstFoundUri(session.getReport() == null ? null : session.getReport().getExperiments())
                .ifPresent(uri -> experiment.suggest(uri.toString(), "AiImport.proposal.from.resolvedExperiment"));
        requirements.field(experiment);

        RequiredField provenance = new RequiredField("provenance_name",
                "AiImport.proposal.field.provenanceName", "text", true);
        provenance.suggest(facts(session).defaultProvenanceName(), "AiImport.proposal.from.fileName");
        requirements.field(provenance);

        requirements.field(new RequiredField("provenance_description",
                "AiImport.proposal.field.provenanceDescription", RequiredField.KIND_LONG_TEXT, false));

        // A decision, not a value: the profile had to recompose the plot identifiers to attach any
        // observation at all, and the user says yes to that before anything is written. Carried as
        // a required checkbox rather than a blocker, because a blocker would take the decision
        // away from them — the card would simply refuse and never ask.
        facts(session).reconciliationNote().ifPresent(note -> {
            RequiredField confirmation = new RequiredField("reconciliation_confirmed",
                    "AiImport.proposal.field.reconciliationConfirmed",
                    RequiredField.KIND_BOOLEAN, true);
            confirmation.suggest(note, "AiImport.proposal.from.profileReconciliation");
            requirements.field(confirmation);
        });

        List<DataPoint> points = session.getDataPoints();
        if (points.isEmpty()) {
            requirements.block("AiImport.proposal.block.noDataPoints");
            return requirements;
        }
        if (points.size() > MAX_DATA_POINTS) {
            requirements.block("AiImport.proposal.block.tooManyDataPoints");
        }

        ResolutionReport report = session.getReport();
        if (report == null) {
            requirements.block("AiImport.proposal.block.notAnalysed");
            return requirements;
        }

        if (facts(session).firstFoundUri(report.getExperiments()).isEmpty()) {
            requirements.block("AiImport.proposal.block.experimentMissing");
        }
        if (facts(session).countStatus(report.getVariables(), ResolutionStatus.MISSING) > 0
                || facts(session).countStatus(report.getVariables(), ResolutionStatus.FOUND_IN_SHARED_RESOURCE) > 0) {
            requirements.block("AiImport.proposal.block.variablesMissing");
        }
        if (facts(session).countStatus(report.getVariables(), ResolutionStatus.AMBIGUOUS) > 0) {
            requirements.block("AiImport.proposal.block.variablesAmbiguous");
        }
        if (facts(session).countStatus(report.getScientificObjects(), ResolutionStatus.MISSING) > 0) {
            requirements.block("AiImport.proposal.block.objectsMissing");
        }
        if (facts(session).countStatus(report.getScientificObjects(), ResolutionStatus.NOT_CHECKED) > 0) {
            requirements.block("AiImport.proposal.block.objectsNotChecked");
        }

        // Only when the file actually measures something at a facility — a weather sheet does, a
        // plot sheet does not, and blocking on a facility no observation points at would be noise.
        boolean measuresAtFacility = points.stream()
                .anyMatch(point -> point.getTargetKind() == DataPoint.TargetKind.FACILITY);
        if (measuresAtFacility
                && facts(session).countStatus(report.getFacilities(), ResolutionStatus.MISSING) > 0) {
            requirements.block("AiImport.proposal.block.facilitiesMissing");
        }

        for (ColumnMapping mapping : session.getMappings()) {
            if (mapping.getRole() == ColumnRole.VARIABLE && mapping.hasIssues()) {
                requirements.warn("AiImport.proposal.warn.typeIssues");
                break;
            }
        }
        return requirements;
    }

    //#endregion

    //#region creation

    /**
     * Imports the variables the report found on a shared resource instance.
     * <p>
     * Delegated to {@link VariableCopyLogic}, which is the code the variables screen runs: copying
     * a variable means copying its entity, characteristic, method and unit too, in one
     * transaction, and a second implementation of that would be a second way to leave it half
     * done.
     *
     * @return how many resources were created here, components included
     */
    public int importVariables(AiImportSession session, Map<String, String> values)
            throws Exception {
        if (coreModule == null) {
            throw new IllegalStateException(
                    "Importing from a shared resource instance needs the core module.");
        }
        VariableCopyLogic copier = new VariableCopyLogic(sparql, coreModule, currentUser);

        int created = 0;
        for (Map.Entry<URI, List<URI>> entry : facts(session).importableVariables().entrySet()) {
            VariableCopyResult result = copier.copy(entry.getKey(), entry.getValue());
            LOGGER.info("Imported {} variables and {} of their components from {}",
                    result.getVariableUris().size(),
                    result.total() - result.getVariableUris().size(), entry.getKey());
            created += result.total();
        }
        return created;
    }

    /**
     * Turns the comma-separated project URIs of the form into models, checking first that they
     * resolve.
     * <p>
     * Checked here rather than left to the SPARQL layer: it does reject an unknown URI, but it
     * rejects it as {@code SPARQLInvalidUriListException} deep in the storage, and what reached the
     * user was a list of URIs with nothing saying which form field to correct.
     */
    private List<ProjectModel> linkedProjects(String projects) throws Exception {
        List<ProjectModel> linked = new ArrayList<>();
        if (projects == null || projects.trim().isEmpty()) {
            return linked;
        }

        List<URI> uris = new ArrayList<>();
        for (String uri : projects.split(",")) {
            String trimmed = uri.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                uris.add(new URI(trimmed));
            } catch (URISyntaxException e) {
                throw new CreationFieldException("projects",
                        "'" + trimmed + "' is not a URI. Pick the project from the list instead of "
                                + "typing it.");
            }
        }
        if (uris.isEmpty()) {
            return linked;
        }

        // checkExist = false returns the ones that are absent, which is what has to be reported.
        Set<URI> unknown = sparql.getExistingUris(ProjectModel.class, uris, false);
        if (!unknown.isEmpty()) {
            throw new CreationFieldException("projects",
                    "No project carries " + (unknown.size() == 1 ? "this URI: " : "these URIs: ")
                            + unknown + ". Pick an existing project, or create it first.");
        }

        uris.forEach(uri -> {
            ProjectModel project = new ProjectModel();
            project.setUri(uri);
            linked.add(project);
        });
        return linked;
    }

    /**
     * Creates the events, or none of them.
     * <p>
     * Same rule as the observations, for the same reason: an event whose target cannot be found is
     * an event filed against nothing, and half a set of sprayings is worse than none.
     *
     * @return how many events were created
     */
    public int createEvents(AiImportSession session, Map<String, String> values) throws Exception {
        List<EventModel> models = new ArrayList<>();
        for (EventCandidate candidate : session.getEvents()) {
            List<URI> targets = facts(session).resolveTargets(candidate);
            if (targets.isEmpty()) {
                // Reported as a warning at requirement time; skipped here rather than failing the
                // whole set, since a trial-wide event with no field is still the exception.
                LOGGER.warn("Skipping the event of {} row {}: no target resolved",
                        candidate.getSheet(), candidate.getRowNumber());
                continue;
            }
            EventModel model = new EventModel();
            model.setType(URI.create(Oeev.Event.getURI()));
            model.setTargets(targets);
            model.setDescription(candidate.toEventDescription());
            model.setIsInstant(true);

            // The templates date events to the day, so the event is an instant and both ends carry
            // the same moment — what the model expects of an instantaneous event.
            InstantModel instant = new InstantModel();
            instant.setDateTimeStamp(candidate.getDate().atStartOfDay().atOffset(ZoneOffset.UTC));
            model.setEnd(instant);

            models.add(model);
        }
        if (models.isEmpty()) {
            return 0;
        }
        new EventLogic<>(sparql, nosql, currentUser, EventModel.class).create(models, false);
        return models.size();
    }


    public URI createProject(AiImportSession session, Map<String, String> values) throws Exception {
        ProjectModel model = new ProjectModel();
        model.setName(required(values, "name"));
        model.setStartDate(requiredDate(values, "start_date"));
        model.setShortname(optional(values, "shortname"));
        model.setObjective(optional(values, "objective"));
        model.setDescription(optional(values, "description"));
        LocalDate endDate = optionalDate(values, "end_date");
        if (endDate != null) {
            model.setEndDate(endDate);
        }

        return new ProjectDAO(sparql).create(model).getUri();
    }

    public URI createExperiment(AiImportSession session, Map<String, String> values) throws Exception {
        ExperimentModel model = new ExperimentModel();
        model.setName(required(values, "name"));
        model.setObjective(required(values, "objective"));
        model.setStartDate(requiredDate(values, "start_date"));
        model.setDescription(optional(values, "description"));
        LocalDate endDate = optionalDate(values, "end_date");
        if (endDate != null) {
            model.setEndDate(endDate);
        }

        model.setProjects(linkedProjects(optional(values, "projects")));

        return new ExperimentDAO(sparql, nosql, fs).create(model).getUri();
    }

    /**
     * Inserts the observations read from the file, or writes nothing at all.
     * <p>
     * Every target and every variable is resolved first. If one row cannot be placed, the whole
     * insertion is refused and the offending rows are named. Writing the rows that happen to
     * resolve would leave the user believing they imported their dataset when they imported part of
     * it, with nothing on screen saying which part.
     */
    public DataInsertionResult insertData(AiImportSession session, Map<String, String> values)
            throws Exception {
        URI experiment = URI.create(required(values, "experiment"));

        List<DataCreationDTO> drafts = new ArrayList<>();
        List<UnresolvedRow> unresolved = resolveRows(session, drafts);

        if (!unresolved.isEmpty()) {
            // Refused before the provenance is created, so a refusal leaves nothing behind.
            LOGGER.warn("Refusing to insert {} observations: {} could not be placed",
                    session.getDataPoints().size(), unresolved.size());
            return DataInsertionResult.refused(
                    unresolved.subList(0, Math.min(unresolved.size(), MAX_UNRESOLVED_REPORTED)),
                    unresolved.size());
        }
        if (drafts.isEmpty()) {
            return DataInsertionResult.inserted(0);
        }

        URI provenance = createProvenance(required(values, "provenance_name"),
                optional(values, "provenance_description"));
        DataProvenanceModel dataProvenance = new DataProvenanceModel();
        dataProvenance.setUri(provenance);
        dataProvenance.setExperiments(List.of(experiment));

        List<DataModel> models = new ArrayList<>(drafts.size());
        for (DataCreationDTO draft : drafts) {
            draft.setProvenance(dataProvenance);
            models.add(draft.newModel());
        }

        new DataLogic(sparql, nosql, fs, currentUser).createMany(models);
        return DataInsertionResult.inserted(models.size());
    }

    private URI createProvenance(String name, String description) throws Exception {
        ProvenanceModel model = new ProvenanceModel();
        model.setName(name);
        model.setDescription(description);
        return new ProvenanceDAO(nosql, sparql).create(model).getUri();
    }

    //#endregion

    //#region lookups built from the report


    /**
     * Resolves every plot the observations mention, in one query.
     * <p>
     * {@code checkUniqueNameByGraph} sends the whole set of names as a SPARQL {@code VALUES} clause
     * and returns them mapped to their URI. One query instead of one per plot — and, more to the
     * point, no reason left to resolve only a sample, which is what used to make observations
     * disappear.
     */
    /**
     * Places every observation on a target and a variable, without writing anything.
     * <p>
     * Separate from the writing so that the decision to refuse can be tested on its own, and so
     * that reading the code makes the order plain: everything resolves, then everything is written.
     *
     * @param drafts filled with what would be written, when nothing is left unresolved
     * @return the rows that could not be placed, empty when the insertion can go ahead
     */
    List<UnresolvedRow> resolveRows(AiImportSession session, List<DataCreationDTO> drafts) {
        Map<String, URI> variablesByColumn = facts(session).variableUrisByColumn();
        Map<String, URI> objectsByName = facts(session).scientificObjectUris();
        Map<String, URI> facilitiesByName = facts(session).facilityUris();

        List<UnresolvedRow> unresolved = new ArrayList<>();
        for (DataPoint point : session.getDataPoints()) {
            URI variable = variablesByColumn.get(point.getVariableKey().toLowerCase());
            if (variable == null) {
                unresolved.add(new UnresolvedRow(point.getSheet(), point.getRowNumber(),
                        point.getVariableKey(), "AiImport.proposal.unresolved.variable",
                        point.getVariableKey()));
                continue;
            }
            boolean atFacility = point.getTargetKind() == DataPoint.TargetKind.FACILITY;
            URI target = atFacility
                    ? facilitiesByName.get(point.getObjectName().toLowerCase())
                    : objectsByName.get(point.getObjectName().toLowerCase());
            if (target == null) {
                unresolved.add(new UnresolvedRow(point.getSheet(), point.getRowNumber(),
                        point.getVariableKey(),
                        atFacility
                                ? "AiImport.proposal.unresolved.facility"
                                : "AiImport.proposal.unresolved.object",
                        point.getObjectName()));
                continue;
            }

            DataCreationDTO dto = new DataCreationDTO();
            dto.setDate(point.getDate().toString());
            dto.setTarget(target);
            dto.setVariable(variable);
            dto.setValue(point.getRawValue());
            drafts.add(dto);
        }
        return unresolved;
    }



    //#endregion

    //#region field access

    private String required(Map<String, String> values, String name) {
        String value = optional(values, name);
        if (value == null) {
            throw new IllegalArgumentException("The field '" + name + "' is required.");
        }
        return value;
    }

    private String optional(Map<String, String> values, String name) {
        if (values == null) {
            return null;
        }
        String value = values.get(name);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LocalDate requiredDate(Map<String, String> values, String name) {
        return parseDate(required(values, name), name);
    }

    private LocalDate optionalDate(Map<String, String> values, String name) {
        String value = optional(values, name);
        return value == null ? null : parseDate(value, name);
    }

    private LocalDate parseDate(String value, String name) {
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "The field '" + name + "' must be a date written as yyyy-MM-dd.");
        }
    }

    //#endregion

    //#region suggestions read from the file







    //#endregion
}
