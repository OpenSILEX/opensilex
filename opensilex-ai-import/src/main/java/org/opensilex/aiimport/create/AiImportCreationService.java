//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create;

import org.opensilex.aiimport.mapping.ColumnMapping;
import org.opensilex.aiimport.mapping.ColumnRole;
import org.opensilex.aiimport.profile.DataPoint;
import org.opensilex.aiimport.create.bulk.DataBulkImport;
import org.opensilex.aiimport.create.bulk.ScientificObjectBulkImport;
import org.opensilex.aiimport.create.objects.ObjectSheet;
import org.opensilex.aiimport.create.objects.ObjectSheets;
import org.opensilex.aiimport.profile.EventCandidate;
import org.opensilex.aiimport.profile.FactorLevelCandidate;
import org.opensilex.aiimport.profile.ImportProfileRegistry;
import org.opensilex.aiimport.profile.ObjectRow;
import org.opensilex.aiimport.resolve.ResolutionReport;
import org.opensilex.aiimport.resolve.ResolutionStatus;
import org.opensilex.aiimport.resolve.ResolvedItem;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.core.CoreModule;
import org.opensilex.core.event.bll.EventLogic;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.api.FactorAPI;
import org.opensilex.core.experiment.factor.dal.FactorDAO;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.project.dal.ProjectDAO;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.core.variable.bll.VariableCopyLogic;
import org.opensilex.core.variable.bll.VariableCopyResult;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ForbiddenURIAccessException;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.sparql.exceptions.SPARQLInvalidUriListException;
import org.opensilex.sparql.model.SPARQLResourceModel;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
     * The most observations one file may insert. The platform imports at most 10,000 lines at
     * once, so a larger file goes in batches, all validated in memory before the first is written;
     * this bounds that memory, and refuses a file above it up front with an explanation.
     */
    public static final int MAX_DATA_POINTS = 50_000;

    /**
     * The fields of a factor creation.
     */
    public static final String FACTOR_EXPERIMENT = "experiment";
    public static final String FACTOR_NAME = "factor_name";

    /**
     * The fields linking an experiment to the organisations running it and the facilities it uses.
     */
    public static final String EXPERIMENT_ORGANIZATIONS = "organisations";
    public static final String EXPERIMENT_FACILITIES = "facilities";

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
            case FACTORS:
                return factorRequirements(session);
            case SCIENTIFIC_OBJECTS:
                return objectRequirements(session);
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

        RequiredField organizations = new RequiredField(EXPERIMENT_ORGANIZATIONS,
                "AiImport.proposal.field.organizations", RequiredField.KIND_URI_LIST, false)
                .setResource(RequiredField.RESOURCE_ORGANIZATION);
        RequiredField facilities = new RequiredField(EXPERIMENT_FACILITIES,
                "AiImport.proposal.field.facilities", RequiredField.KIND_URI_LIST, false)
                .setResource(RequiredField.RESOURCE_FACILITY);
        if (session.getReport() != null) {
            foundUris(session.getReport().getOrganizations()).ifPresent(uris ->
                    organizations.suggest(uris, "AiImport.proposal.from.resolvedOrganizations"));
            foundUris(session.getReport().getFacilities()).ifPresent(uris ->
                    facilities.suggest(uris, "AiImport.proposal.from.resolvedFacilities"));
        }
        requirements.field(organizations);
        requirements.field(facilities);

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
     * What creating the observed objects takes: the experiment they belong to, and their type.
     * <p>
     * The type is asked, never guessed. No template states it — a plot sheet counts plants per plot
     * without saying what a plot is — and several hundred objects created under a type nobody chose
     * are not repaired by editing one of them.
     */
    private CreationRequirements objectRequirements(AiImportSession session) {
        CreationRequirements requirements = new CreationRequirements(CreationTarget.SCIENTIFIC_OBJECTS);
        List<ObjectRow> rows = ScientificObjectBulkImport.rowsOf(session);
        if (rows.isEmpty()) {
            requirements.block("AiImport.proposal.block.noObjectRows");
            return requirements;
        }

        RequiredField experiment = new RequiredField(ScientificObjectBulkImport.EXPERIMENT,
                "AiImport.proposal.field.experiment", RequiredField.KIND_URI, true)
                .setResource(RequiredField.RESOURCE_EXPERIMENT);
        facts(session).firstFoundUri(session.getReport() == null ? null : session.getReport().getExperiments())
                .ifPresent(uri -> experiment.suggest(uri.toString(), "AiImport.proposal.from.resolvedExperiment"));
        requirements.field(experiment);

        // Each sheet carries its own type, chosen in the object sheets panel or stated by the file;
        // this one only stands for the sheets that have none, and is asked only when one hasn't.
        List<ObjectSheet> sheets = ObjectSheets.of(session).stream().filter(ObjectSheet::isIncluded)
                .collect(Collectors.toList());
        boolean untyped = sheets.isEmpty() || sheets.stream().anyMatch(sheet -> sheet.getType() == null);
        requirements.field(new RequiredField(ScientificObjectBulkImport.OBJECT_TYPE,
                "AiImport.proposal.field.objectType", RequiredField.KIND_URI, untyped)
                .setResource(RequiredField.RESOURCE_OBJECT_TYPE));

        long included = sheets.stream().mapToLong(sheet -> sheet.getRows().size()).sum();
        String summary = sheets.size() > 1
                ? included + " (" + sheets.stream().map(sheet -> sheet.getName() + " : " + sheet.getRows().size())
                .collect(Collectors.joining(", ")) + ")"
                : String.valueOf(sheets.isEmpty() ? rows.size() : included);
        requirements.field(new RequiredField("object_summary", "AiImport.proposal.field.objectSummary",
                RequiredField.KIND_TEXT, false)
                .suggest(summary, "AiImport.proposal.from.objectSheet"));
        if (sheets.stream().anyMatch(ObjectSheet::isTypeFromFile)) {
            requirements.warn("AiImport.proposal.warn.objectTypeFromFile");
        }

        // The importer updates an object whose name already exists in the experiment: said before
        // the click, since an update is not what "create" suggests.
        if (session.getReport() != null && facts(session).countStatus(
                session.getReport().getScientificObjects(), ResolutionStatus.FOUND) > 0) {
            requirements.warn("AiImport.proposal.warn.objectsWillBeUpdated");
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
     * What creating the experiment's factors takes: the experiment they belong to — a factor is
     * declared in one, which is why it has to exist first — and the factor's name when the file
     * does not give it, which a STAR workbook never does.
     */
    private CreationRequirements factorRequirements(AiImportSession session) {
        CreationRequirements requirements = new CreationRequirements(CreationTarget.FACTORS);
        List<FactorLevelCandidate> levels = factorLevelsOf(session);
        if (levels.isEmpty()) {
            requirements.block("AiImport.proposal.block.noFactorLevels");
            return requirements;
        }
        if (!mayCreateFactors()) {
            requirements.block("AiImport.proposal.block.factorsNotAllowed");
        }

        RequiredField experiment = new RequiredField(FACTOR_EXPERIMENT, "AiImport.proposal.field.experiment",
                RequiredField.KIND_URI, true).setResource(RequiredField.RESOURCE_EXPERIMENT);
        Optional<URI> resolved = facts(session).firstFoundUri(
                session.getReport() == null ? null : session.getReport().getExperiments());
        resolved.ifPresent(uri -> experiment.suggest(uri.toString(), "AiImport.proposal.from.resolvedExperiment"));
        requirements.field(experiment);
        if (!resolved.isPresent()) {
            requirements.warn("AiImport.proposal.warn.experimentFirst");
        }

        boolean named = levels.stream().allMatch(level -> level.factor() != null);
        requirements.field(new RequiredField(FACTOR_NAME, "AiImport.proposal.field.factorName",
                RequiredField.KIND_TEXT, !named));

        String codes = levels.stream().limit(12).map(FactorLevelCandidate::code).collect(Collectors.joining(", "));
        requirements.field(new RequiredField("levels_summary", "AiImport.proposal.field.levelsSummary",
                RequiredField.KIND_TEXT, false)
                .suggest(levels.size() + " : " + codes + (levels.size() > 12 ? ", …" : ""),
                        "AiImport.proposal.from.treatmentSheet"));
        return requirements;
    }

    /**
     * The URIs of the items the report found, comma-separated as a list field carries them.
     */
    private static Optional<String> foundUris(List<ResolvedItem> items) {
        String uris = items.stream().filter(item -> item.getStatus() == ResolutionStatus.FOUND)
                .filter(item -> !item.getMatches().isEmpty())
                .map(item -> item.getMatches().get(0).getUri().toString())
                .distinct().collect(Collectors.joining(","));
        return uris.isEmpty() ? Optional.empty() : Optional.of(uris);
    }

    private List<FactorLevelCandidate> factorLevelsOf(AiImportSession session) {
        if (session.getWorkbook() == null) {
            return List.of();
        }
        return new ImportProfileRegistry().getById(session.getProfileId())
                .map(profile -> profile.extractFactorLevels(session.getWorkbook()))
                .orElse(List.of());
    }

    /**
     * The right the platform's factor screen asks for.
     */
    private boolean mayCreateFactors() {
        if (currentUser == null) {
            return false;
        }
        if (Boolean.TRUE.equals(currentUser.isAdmin())) {
            return true;
        }
        try {
            return new AccountDAO(sparql).getCredentialList(currentUser.getUri())
                    .contains(FactorAPI.CREDENTIAL_FACTOR_MODIFICATION_ID);
        } catch (Exception e) {
            LOGGER.warn("Could not read the credentials of {}", currentUser.getUri(), e);
            return false;
        }
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
     * Carries out a confirmed draft: the one place that knows which operation serves which target.
     * <p>
     * The requirements are checked by the caller beforehand; this only writes. A refusal comes back
     * as the rows of the workbook in the way, and means nothing was written.
     */
    public CreationOutcome apply(CreationTarget target, AiImportSession session,
                                 Map<String, String> values) throws Exception {
        switch (target) {
            case PROJECT:
                return CreationOutcome.created(createProject(session, values));
            case EXPERIMENT:
                return CreationOutcome.created(createExperiment(session, values));
            case FACTORS:
                return CreationOutcome.inserted(createFactors(session, values));
            case SCIENTIFIC_OBJECTS:
                return CreationOutcome.of(new ScientificObjectBulkImport(sparql, nosql, fs, currentUser)
                        .importAll(session, values));
            case VARIABLE:
                return CreationOutcome.inserted(importVariables(session, values));
            case EVENT:
                return CreationOutcome.inserted(createEvents(session, values));
            case DATA:
            default:
                // Through the platform's data import: its validation, its batch history, its
                // archived CSV — see DataBulkImport.
                return CreationOutcome.of(new DataBulkImport(sparql, nosql, fs, currentUser)
                        .importAll(session, values));
        }
    }

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
        return linked("projects", projects, ProjectModel.class, ProjectModel::new, "project");
    }

    /**
     * The resources a field lists, by URI, each checked to exist: refused on that field otherwise,
     * rather than deep in the storage where nothing would say which field to correct.
     */
    private <T extends SPARQLResourceModel> List<T> linked(String field, String value, Class<T> type,
                                                           Supplier<T> constructor, String noun)
            throws Exception {
        List<T> linked = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) {
            return linked;
        }

        List<URI> uris = new ArrayList<>();
        for (String uri : value.split(",")) {
            String trimmed = uri.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                uris.add(new URI(trimmed));
            } catch (URISyntaxException e) {
                throw new CreationFieldException(field,
                        "'" + trimmed + "' is not a URI. Pick the " + noun + " from the list instead of "
                                + "typing it.");
            }
        }
        if (uris.isEmpty()) {
            return linked;
        }

        // checkExist = false returns the ones that are absent, which is what has to be reported.
        Set<URI> unknown = sparql.getExistingUris(type, uris, false);
        if (!unknown.isEmpty()) {
            throw new CreationFieldException(field,
                    "No " + noun + " carries " + (unknown.size() == 1 ? "this URI: " : "these URIs: ")
                            + unknown + ". Pick an existing " + noun + ", or create it first.");
        }

        uris.forEach(uri -> {
            T resource = constructor.get();
            resource.setUri(uri);
            linked.add(resource);
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
        // The organisations running the trial and the field it stands in: the platform only lets a
        // plot be hosted by a facility the experiment uses or its organisations host.
        model.setOrganizations(linked(EXPERIMENT_ORGANIZATIONS, optional(values, EXPERIMENT_ORGANIZATIONS),
                OrganizationModel.class, OrganizationModel::new, "organisation"));
        model.setFacilities(linked(EXPERIMENT_FACILITIES, optional(values, EXPERIMENT_FACILITIES),
                FacilityModel.class, FacilityModel::new, "facility"));

        return new ExperimentDAO(sparql, nosql, fs).create(model).getUri();
    }

    /**
     * Declares the treatments of the workbook as the levels of the experiment's factors, the way
     * the platform's factor screen does: the level's name is the treatment's code — what the object
     * sheets write, and what the objects are matched on — its description what the file says of it.
     * <p>
     * A treatment the experiment already has is left as it is; a factor the experiment already
     * names is refused on its field rather than duplicated under the same name.
     *
     * @return how many levels were created
     */
    public int createFactors(AiImportSession session, Map<String, String> values) throws Exception {
        if (!mayCreateFactors()) {
            throw new CreationFieldException(FACTOR_EXPERIMENT,
                    "Creating factors needs the right to modify factors on this instance.");
        }
        ExperimentModel experiment = experimentOf(required(values, FACTOR_EXPERIMENT));
        FactorDAO dao = new FactorDAO(sparql);

        Map<String, FactorModel> existingFactors = new HashMap<>();
        Set<String> existingLevels = new HashSet<>();
        for (FactorModel factor : dao.getByExperiment(experiment.getUri(), currentUser.getLanguage())) {
            existingFactors.put(factor.getName().toLowerCase(Locale.ROOT), factor);
            if (factor.getFactorLevels() != null) {
                factor.getFactorLevels().forEach(level -> existingLevels.add(level.getName().toLowerCase(Locale.ROOT)));
            }
        }

        String defaultName = optional(values, FACTOR_NAME);
        Map<String, List<FactorLevelCandidate>> byFactor = new LinkedHashMap<>();
        for (FactorLevelCandidate level : factorLevelsOf(session)) {
            String factor = level.factor() != null ? level.factor() : defaultName;
            if (factor == null) {
                throw new CreationFieldException(FACTOR_NAME, "The file does not name the factor its "
                        + "treatments belong to: give it a name.");
            }
            byFactor.computeIfAbsent(factor, name -> new ArrayList<>()).add(level);
        }

        int created = 0;
        for (Map.Entry<String, List<FactorLevelCandidate>> factor : byFactor.entrySet()) {
            List<FactorLevelCandidate> fresh = factor.getValue().stream()
                    .filter(level -> !existingLevels.contains(level.code().toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
            if (fresh.isEmpty()) {
                continue;
            }
            if (existingFactors.containsKey(factor.getKey().toLowerCase(Locale.ROOT))) {
                throw new CreationFieldException(FACTOR_NAME, "The experiment already has a factor named '"
                        + factor.getKey() + "'. Name this one differently, or add the missing levels to "
                        + "that factor from the experiment's page.");
            }
            FactorModel model = new FactorModel();
            model.setName(factor.getKey());
            model.setPublisher(currentUser.getUri());
            List<FactorLevelModel> levels = new ArrayList<>();
            for (FactorLevelCandidate candidate : fresh) {
                FactorLevelModel level = new FactorLevelModel();
                level.setName(candidate.code());
                level.setDescription(candidate.levelDescription());
                level.setPublisher(currentUser.getUri());
                level.setFactor(model);
                levels.add(level);
            }
            model.setFactorLevels(levels);
            model.setExperiment(experiment);
            model.setAssociatedExperiments(List.of(experiment));
            dao.create(model);
            created += levels.size();
        }
        return created;
    }

    /**
     * The experiment a field names, as the user may see it; refused on that field otherwise.
     */
    private ExperimentModel experimentOf(String value) throws Exception {
        URI uri;
        try {
            uri = new URI(value.trim());
        } catch (URISyntaxException e) {
            throw new CreationFieldException(FACTOR_EXPERIMENT, "'" + value + "' is not a URI. Pick the "
                    + "experiment from the list instead of typing it.");
        }
        try {
            return new ExperimentDAO(sparql, nosql, fs).get(uri, currentUser);
        } catch (NotFoundURIException | ForbiddenURIAccessException | SPARQLInvalidUriListException e) {
            throw new CreationFieldException(FACTOR_EXPERIMENT, "No experiment you can see carries this URI: "
                    + uri + ". Create the experiment first.");
        }
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
