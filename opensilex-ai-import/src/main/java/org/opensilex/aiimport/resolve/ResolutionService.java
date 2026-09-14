//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.resolve;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_Str;
import org.apache.jena.sparql.expr.E_StrEndsWith;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.opensilex.aiimport.profile.ExtractedImportPlan;
import org.opensilex.aiimport.profile.PersonCandidate;
import org.opensilex.aiimport.profile.VariableCandidate;
import org.opensilex.aiimport.profile.VariableComponent;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.germplasm.api.GermplasmSearchFilter;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.organisation.bll.FacilityLogic;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.facility.FacilitySearchFilter;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.project.dal.ProjectDAO;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.variable.dal.BaseVariableDAO;
import org.opensilex.core.variable.dal.BaseVariableModel;
import org.opensilex.core.variable.dal.CharacteristicModel;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.security.person.dal.PersonDAO;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.core.variable.dal.VariableSearchFilter;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.apache.jena.arq.querybuilder.AbstractQueryBuilder.makeVar;

/**
 * Checks, against this instance, every name a profile read out of an uploaded file.
 * <p>
 * The whole point of this class is that no URI ever originates from the language model: the
 * assistant is handed the report this service produces, and can only cite what a database returned.
 *
 * @author Arnaud Charleroy
 */
public class ResolutionService {

    /**
     * Translation key prefixes. The English sentence travels with the key, because the same
     * sentence goes into the prompt where a key would mean nothing.
     */
    private static final String HINT = "AiImport.report.hint.";
    private static final String WARN = "AiImport.report.warning.";

    /**
     * How many people one lookup brings back. A name is searched as a pattern, so a common one can
     * match several; more than a handful means the file gave a name too vague to settle here.
     */
    private static final int PERSON_SEARCH_LIMIT = 10;

    /**
     * How many candidates a component lookup brings back before the exact name is picked out. A
     * handful: the search is a label pattern, and only an exact name is accepted from it.
     */
    private static final int COMPONENT_SEARCH_LIMIT = 10;

    private static final Logger LOGGER = LoggerFactory.getLogger(ResolutionService.class);

    private final SPARQLService sparql;
    private final MongoDBService nosql;
    private final FileStorageService fs;
    private final AccountModel currentUser;
    private final SharedResourceVariableLookup sharedResources;

    public ResolutionService(SPARQLService sparql,
                             MongoDBService nosql,
                             FileStorageService fs,
                             AccountModel currentUser,
                             SharedResourceVariableLookup sharedResources) {
        this.sparql = sparql;
        this.nosql = nosql;
        this.fs = fs;
        this.currentUser = currentUser;
        this.sharedResources = sharedResources;
    }

    public ResolutionReport resolve(ExtractedImportPlan plan) {
        ResolutionReport report = new ResolutionReport().setProfileId(plan.getProfileId());
        report.addAnomalies(plan.getAnomalyMessages());
        report.getNotes().putAll(plan.getNotes());

        List<ExperimentModel> resolvedExperiments = resolveExperiments(plan, report);
        resolveProjects(plan, report);
        resolveVariables(plan, report);
        resolveGermplasm(plan, report);
        resolvePersons(plan, report);
        resolveScientificObjects(plan, report, resolvedExperiments);
        resolveFacilities(plan, report);

        return report;
    }

    //#region experiments

    private List<ExperimentModel> resolveExperiments(ExtractedImportPlan plan, ResolutionReport report) {
        List<ExperimentModel> resolved = new ArrayList<>();
        ExperimentDAO dao = new ExperimentDAO(sparql, nosql, fs);

        for (String name : plan.getExperimentNames()) {
            ResolvedItem item = new ResolvedItem(name);
            report.getExperiments().add(item);
            try {
                ExperimentModel experiment = dao.getExperimentByNameOrURI(name, currentUser);
                if (experiment == null) {
                    item.setStatus(ResolutionStatus.MISSING)
                            .setHint(ReportMessage.of(HINT + "experimentMissing",
                                    "No experiment carries this name. Create it, or tell the "
                                            + "assistant which existing experiment the trial "
                                            + "corresponds to."));
                } else {
                    item.setStatus(ResolutionStatus.FOUND)
                            .getMatches().add(reference(experiment));
                    resolved.add(experiment);
                }
            } catch (Exception e) {
                // A duplicate name lands here: the DAO refuses to choose, and so do we.
                LOGGER.debug("Could not resolve experiment {}", name, e);
                item.setStatus(ResolutionStatus.AMBIGUOUS)
                        .setHint(ReportMessage.of(HINT + "experimentAmbiguous",
                                "Several experiments carry this name, so the right one cannot be "
                                        + "chosen automatically."));
            }
        }
        return resolved;
    }

    //#endregion

    //#region projects

    private void resolveProjects(ExtractedImportPlan plan, ResolutionReport report) {
        ProjectDAO dao = new ProjectDAO(sparql);

        for (String name : plan.getProjectNames()) {
            ResolvedItem item = new ResolvedItem(name);
            report.getProjects().add(item);
            try {
                List<ProjectModel> candidates = dao
                        .search(name, null, null, null, currentUser, null, 0, 10)
                        .getList();
                List<ResourceReference> exact = new ArrayList<>();
                for (ProjectModel project : candidates) {
                    if (name.equalsIgnoreCase(project.getName())
                            || name.equalsIgnoreCase(project.getShortname())) {
                        exact.add(reference(project));
                    }
                }
                applyMatches(item, exact, ReportMessage.of(HINT + "projectMissing",
                        "No project carries this name. Ask the user which project the trial belongs "
                                + "to, or create it."));
            } catch (Exception e) {
                LOGGER.warn("Could not resolve project {}", name, e);
                report.addWarning(ReportMessage.of(WARN + "projectLookupFailed",
                        "The project '" + name + "' could not be looked up.").with("name", name));
            }
        }
    }

    //#endregion

    //#region variables

    private void resolveVariables(ExtractedImportPlan plan, ResolutionReport report) {
        VariableDAO dao = new VariableDAO(sparql, nosql, fs, currentUser);

        for (VariableCandidate candidate : plan.getVariables()) {
            ResolvedItem item = new ResolvedItem(candidate.getColumnKey())
                    .setExternalId(candidate.getExternalId());
            report.getVariables().add(item);

            List<ResourceReference> matches = new ArrayList<>();

            // The ontology identifier the file carries is the most reliable key, so try it first.
            if (candidate.getExternalId() != null && !candidate.getExternalId().isEmpty()) {
                matches.addAll(searchByExactMatch(candidate.getExternalId(), report));
            }
            if (!matches.isEmpty()) {
                // The ontology query returns uri and name only; the datatype comes from the model.
                fetchDatatypes(dao, matches, report);
            } else {
                matches.addAll(searchByName(dao, candidate, report));
            }

            if (!matches.isEmpty()) {
                applyMatches(item, matches, null);
                continue;
            }

            List<ResourceReference> remote = sharedResources != null && sharedResources.isEnable()
                    ? sharedResources.search(candidate.getSearchName(), report.getWarnings())
                    : new ArrayList<>();
            if (!remote.isEmpty()) {
                item.setStatus(ResolutionStatus.FOUND_IN_SHARED_RESOURCE)
                        .setMatches(remote)
                        .setHint(ReportMessage.of(HINT + "variableInSharedResource",
                                "This variable exists on a shared resource instance. It can be "
                                        + "imported from here, with its entity, characteristic, "
                                        + "method and unit, in one step."));
                continue;
            }

            // Nowhere to be found, so the next step is creating it — and that takes four
            // components. Whatever the file says about them is resolved now: a component that
            // already exists here must be reused, never entered a second time.
            resolveComponents(candidate, item);

            item.setStatus(ResolutionStatus.MISSING)
                    .setHint(missingVariableHint(candidate));
        }

        if (!plan.getVariables().isEmpty()) {
            report.getNotes().put("variable columns", String.valueOf(plan.getVariables().size()));
        }
    }

    private ReportMessage missingVariableHint(VariableCandidate candidate) {
        String externalId = candidate.getExternalId();
        if (externalId != null && !externalId.isEmpty()) {
            return ReportMessage.of(HINT + "variableMissingWithExternalId",
                            "No variable matches this column. The file gives the ontology "
                                    + "identifier " + externalId + ", which can be used to create "
                                    + "it.")
                    .with("externalId", externalId);
        }
        return ReportMessage.of(HINT + "variableMissing",
                "No variable matches this column. Ask the user what it measures, with which "
                        + "method and in which unit.");
    }

    private List<ResourceReference> searchByName(VariableDAO dao, VariableCandidate candidate,
                                                 ResolutionReport report) {
        List<ResourceReference> matches = new ArrayList<>();
        for (String name : distinct(candidate.getColumnKey(), candidate.getLabel())) {
            try {
                VariableSearchFilter filter = new VariableSearchFilter()
                        .setNamePattern(name)
                        .setUserModel(currentUser);
                filter.setLang(currentUser.getLanguage());
                filter.setPage(0);
                filter.setPageSize(10);

                for (VariableModel variable : dao.search(filter).getList()) {
                    if (!isExactVariableMatch(name, variable)) {
                        continue;
                    }
                    ResourceReference reference = reference(variable)
                            .setDatatype(variable.getDataType() == null
                                    ? null
                                    : variable.getDataType().toString());
                    if (!containsUri(matches, reference.getUri())) {
                        matches.add(reference);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Could not search variable {}", name, e);
                report.addWarning(ReportMessage.of(WARN + "variableLookupFailed",
                        "The variable '" + name + "' could not be looked up locally.")
                        .with("name", name));
            }
            if (!matches.isEmpty()) {
                break;
            }
        }
        return matches;
    }

    /**
     * The name filter of the variable DAO is a regex OR'd over several fields, so it returns near
     * misses. Only an exact name or alternative name is accepted, otherwise the report would point
     * the user at a variable that merely looks similar.
     */
    private boolean isExactVariableMatch(String name, VariableModel variable) {
        return name.equalsIgnoreCase(variable.getName())
                || name.equalsIgnoreCase(variable.getAlternativeName());
    }

    /**
     * Finds variables whose {@code skos:exactMatch} ends with the identifier written in the file.
     * <p>
     * The file carries a compact identifier such as {@code CO_356:1000217} while the instance
     * stores a full URI, and the prefix used is a local choice. Matching on the end of the URI is
     * what bridges the two without hard-coding anyone's prefix.
     */
    private List<ResourceReference> searchByExactMatch(String externalId, ResolutionReport report) {
        List<ResourceReference> matches = new ArrayList<>();
        try {
            Node variableGraph = sparql.getDefaultGraph(VariableModel.class);
            Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
            Var nameVar = makeVar(SPARQLNamedResourceModel.NAME_FIELD);
            Var matchVar = makeVar("exactMatch");

            SelectBuilder select = new SelectBuilder()
                    .addVar(uriVar)
                    .addVar(nameVar)
                    .setDistinct(true);
            select.addGraph(variableGraph, uriVar, RDF.type, Oeso.Variable);
            select.addGraph(variableGraph, uriVar, RDFS.label, nameVar);
            select.addGraph(variableGraph, uriVar, SKOS.exactMatch, matchVar);
            select.addFilter(new E_StrEndsWith(
                    new E_Str(new ExprVar(matchVar)),
                    new NodeValueString(externalId)));
            select.setLimit(10);

            for (SPARQLResult result : sparql.executeSelectQuery(select, null)) {
                URI uri = URI.create(result.getStringValue(SPARQLResourceModel.URI_FIELD));
                String name = result.getStringValue(SPARQLNamedResourceModel.NAME_FIELD);
                if (!containsUri(matches, uri)) {
                    matches.add(new ResourceReference(uri, name));
                }
            }
        } catch (SPARQLException | RuntimeException e) {
            LOGGER.warn("Could not search variables by exact match on {}", externalId, e);
            report.addWarning(ReportMessage.of(WARN + "externalIdLookupFailed",
                    "Variables could not be looked up by the ontology identifier "
                            + externalId + ".").with("externalId", externalId));
        }
        return matches;
    }

    //#endregion

    /**
     * Fills in the datatype of variables matched by their ontology identifier, which the SPARQL
     * query does not return.
     */
    private void fetchDatatypes(VariableDAO dao, List<ResourceReference> matches,
                                ResolutionReport report) {
        List<URI> uris = new ArrayList<>(matches.size());
        matches.forEach(match -> uris.add(match.getUri()));
        try {
            for (VariableModel variable : dao.getList(uris, currentUser.getLanguage())) {
                for (ResourceReference match : matches) {
                    if (match.getUri() != null && match.getUri().equals(variable.getUri())) {
                        match.setDatatype(variable.getDataType() == null
                                ? null
                                : variable.getDataType().toString());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Could not read the datatype of matched variables", e);
            report.addWarning(ReportMessage.of(WARN + "datatypeUnreadable",
                    "The expected data type of some variables could not be read, so type "
                            + "mismatches were not checked for them."));
        }
    }

    //#region germplasm

    private void resolveGermplasm(ExtractedImportPlan plan, ResolutionReport report) {
        GermplasmDAO dao = new GermplasmDAO(sparql, nosql);

        for (String name : plan.getGermplasmNames()) {
            ResolvedItem item = new ResolvedItem(name);
            report.getGermplasm().add(item);
            try {
                GermplasmSearchFilter filter = new GermplasmSearchFilter();
                filter.setName(name);
                filter.setUser(currentUser);
                filter.setLang(currentUser.getLanguage());
                filter.setPage(0);
                filter.setPageSize(10);

                List<ResourceReference> exact = new ArrayList<>();
                for (GermplasmModel germplasm : dao.search(filter, false, false).getList()) {
                    if (name.equalsIgnoreCase(germplasm.getName())) {
                        exact.add(reference(germplasm));
                    }
                }
                applyMatches(item, exact, ReportMessage.of(HINT + "germplasmMissing",
                        "No germplasm carries this name. Check the naming convention with the user "
                                + "before creating anything."));
            } catch (Exception e) {
                LOGGER.warn("Could not resolve germplasm {}", name, e);
                report.addWarning(ReportMessage.of(WARN + "germplasmLookupFailed",
                        "The germplasm '" + name + "' could not be looked up.").with("name", name));
            }
        }
    }

    //#endregion

    //#region facilities

    /**
     * A field named in the file is a place, and places outlive experiments — so it is looked up
     * across the instance rather than inside one experiment's graph.
     * <p>
     * Through {@code FacilityLogic} rather than a direct query: it computes the organisations and
     * sites the account may see and filters on them. Querying the triplestore straight would list
     * facilities the user has no right to, which is not the resolver's call to make.
     */
    /**
     * Matches the parts a variable is made of against the ones this instance already has.
     * <p>
     * Only what the file actually states. MIAPPE gives a trait, a method and a scale; the scale is
     * the unit and the method is the method, but the trait covers both the entity and the
     * characteristic — "plant height" is the entity "plant" and the characteristic "height" — and
     * that split is a judgement about the user's science. So the trait is offered as a
     * characteristic, which is the closer of the two, and the entity is left for the user to
     * choose. Guessing it would put a wrong entity in the instance's referential for good.
     */
    private void resolveComponents(VariableCandidate candidate, ResolvedItem item) {
        if (!candidate.hasComponents()) {
            return;
        }
        add(item, "characteristic", candidate.getTrait(), CharacteristicModel.class);
        add(item, "method", candidate.getMethod(), MethodModel.class);
        add(item, "unit", candidate.getUnit(), UnitModel.class);
    }

    private <T extends BaseVariableModel<T>> void add(ResolvedItem item, String role,
                                                      VariableComponent component,
                                                      Class<T> modelClass) {
        if (component == null || component.isEmpty()) {
            return;
        }
        ResolvedComponent resolved =
                new ResolvedComponent(role, component.getName(), component.getAccession());
        item.getComponents().add(resolved);

        if (component.getName() == null || component.getName().isEmpty()) {
            return;
        }
        try {
            new BaseVariableDAO<>(modelClass, sparql)
                    .search(Pattern.quote(component.getName()), null, 0, COMPONENT_SEARCH_LIMIT,
                            currentUser.getLanguage())
                    .getList().stream()
                    .filter(model -> component.getName().equalsIgnoreCase(model.getName()))
                    .findFirst()
                    .ifPresent(model -> resolved.setUri(model.getUri()));
        } catch (Exception e) {
            // Not worth a warning of its own: the variable is already reported missing, and an
            // unresolved component only means one more choice on the form.
            LOGGER.warn("Could not look up the {} '{}'", role, component.getName(), e);
        }
    }

    //#endregion

    //#region persons

    /**
     * Matches the people the file names against the ones this instance knows.
     * <p>
     * By the best key each candidate has: an ORCID identifies one human worldwide, an email one
     * mailbox, a name neither. Two agronomists called Martin are not the same person, so a match on
     * the name alone is reported as ambiguous rather than picked.
     */
    private void resolvePersons(ExtractedImportPlan plan, ResolutionReport report) {
        if (plan.getPersons().isEmpty()) {
            return;
        }
        PersonDAO dao = new PersonDAO(sparql);

        for (PersonCandidate candidate : plan.getPersons()) {
            ResolvedItem item = new ResolvedItem(candidate.getName())
                    .setExternalId(candidate.getOrcid());
            report.getPersons().add(item);
            try {
                applyMatches(item, findPerson(dao, candidate),
                        ReportMessage.of(HINT + "personMissing",
                                "Nobody here matches this person. Create them, or say which "
                                        + "existing person they are."));
            } catch (Exception e) {
                LOGGER.warn("Could not resolve person {}", candidate.getName(), e);
                report.addWarning(ReportMessage.of(WARN + "personLookupFailed",
                                "The person '" + candidate.getName() + "' could not be looked up.")
                        .with("name", candidate.getName()));
            }
        }
    }

    /**
     * @return the people matching the candidate's strongest identifier, exact matches only
     */
    private List<ResourceReference> findPerson(PersonDAO dao, PersonCandidate candidate)
            throws Exception {
        List<ResourceReference> matches = new ArrayList<>();
        String key = candidate.getSearchKey();
        if (key == null || key.isEmpty()) {
            return matches;
        }

        // The DAO's pattern runs over the given name, family name, email and ORCID at once, so one
        // query covers whichever key the file supplied.
        for (PersonModel person : dao.search(Pattern.quote(key), null, 0, PERSON_SEARCH_LIMIT)
                .getList()) {
            if (isTheSamePerson(person, candidate)) {
                matches.add(new ResourceReference(person.getUri(), fullNameOf(person)));
            }
        }
        return matches;
    }

    /**
     * A regex search matches loosely; this is where it is made exact again.
     */
    private boolean isTheSamePerson(PersonModel person, PersonCandidate candidate) {
        if (candidate.getOrcid() != null && person.getOrcid() != null) {
            return candidate.getOrcid().equalsIgnoreCase(person.getOrcid().toString());
        }
        if (candidate.getEmail() != null && person.getEmail() != null) {
            return candidate.getEmail().equalsIgnoreCase(person.getEmail().toString());
        }
        return candidate.getName() != null
                && candidate.getName().equalsIgnoreCase(fullNameOf(person));
    }

    /**
     * A person is stored as a given name and a family name; the file writes them together.
     */
    private String fullNameOf(PersonModel person) {
        return (nullToEmpty(person.getFirstName()) + " " + nullToEmpty(person.getLastName())).trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    //#endregion

    //#region facilities

    private void resolveFacilities(ExtractedImportPlan plan, ResolutionReport report) {
        if (plan.getFacilityNames().isEmpty()) {
            return;
        }
        FacilityLogic facilityLogic;
        try {
            facilityLogic = new FacilityLogic(sparql, nosql, currentUser, fs);
        } catch (Exception e) {
            LOGGER.warn("Could not reach the facilities", e);
            report.addWarning(ReportMessage.of(WARN + "facilitiesLookupFailed",
                    "The facilities could not be looked up."));
            return;
        }

        for (String name : plan.getFacilityNames()) {
            ResolvedItem item = new ResolvedItem(name);
            report.getFacilities().add(item);
            try {
                FacilitySearchFilter filter = new FacilitySearchFilter()
                        .setUser(currentUser)
                        .setPattern(name);
                filter.setLang(currentUser.getLanguage());
                filter.setPage(0);
                filter.setPageSize(10);

                List<ResourceReference> exact = new ArrayList<>();
                for (FacilityModel facility : facilityLogic.search(filter).getList()) {
                    // The pattern is a regex, so it returns near misses too.
                    if (name.equalsIgnoreCase(facility.getName())) {
                        exact.add(reference(facility));
                    }
                }
                applyMatches(item, exact, ReportMessage.of(HINT + "facilityMissing",
                        "No facility carries this name. Create it from the facilities screen, or "
                                + "tell the assistant which existing one the field corresponds "
                                + "to."));
            } catch (Exception e) {
                LOGGER.warn("Could not resolve facility {}", name, e);
                report.addWarning(ReportMessage.of(WARN + "facilityLookupFailed",
                        "The facility '" + name + "' could not be looked up.").with("name", name));
            }
        }
    }

    //#endregion

    //#region scientific objects

    private void resolveScientificObjects(ExtractedImportPlan plan, ResolutionReport report,
                                          List<ExperimentModel> experiments) {
        if (plan.getScientificObjectNames().isEmpty()) {
            return;
        }
        if (experiments.isEmpty()) {
            ResolvedItem item = new ResolvedItem(summarise(plan.getScientificObjectNames()))
                    .setStatus(ResolutionStatus.NOT_CHECKED)
                    .setHint(ReportMessage.of(HINT + "objectsNeedAnExperiment",
                            "Scientific objects live inside an experiment, and no experiment could "
                                    + "be resolved yet. Settle the experiment first, then "
                                    + "revalidate."));
            report.getScientificObjects().add(item);
            return;
        }

        // The same plot named twice in the file is one object to look up, and one line in the
        // report.
        List<String> names = new ArrayList<>(new LinkedHashSet<>(plan.getScientificObjectNames()));

        // One query for every name, through the same helper the CSV importer uses. Resolving a
        // sample used to leave the rest absent from the report — not missing, simply unmentioned —
        // and their observations were then dropped without a word at insertion time.
        Map<String, URI> byName;
        try {
            List<ScientificObjectModel> probes = new ArrayList<>(names.size());
            for (String name : names) {
                ScientificObjectModel probe = new ScientificObjectModel();
                probe.setName(name);
                probes.add(probe);
            }
            byName = new ScientificObjectDAO(sparql)
                    .checkUniqueNameByGraph(probes, experiments.get(0).getUri());
        } catch (Exception e) {
            LOGGER.warn("Could not resolve the scientific objects", e);
            report.addWarning(ReportMessage.of(WARN + "objectsLookupFailed",
                    "The scientific objects could not be looked up."));
            return;
        }

        for (String name : names) {
            ResolvedItem item = new ResolvedItem(name);
            report.getScientificObjects().add(item);

            URI uri = byName.get(name);
            if (uri == null) {
                item.setStatus(ResolutionStatus.MISSING)
                        .setHint(ReportMessage.of(HINT + "objectMissing",
                                "No scientific object carries this name in the experiment."));
            } else {
                item.setStatus(ResolutionStatus.FOUND)
                        .getMatches().add(new ResourceReference(uri, name));
            }
        }
        // One thing is given up here, knowingly: checkUniqueNameByGraph keeps the first URI when
        // a name appears twice in the experiment, where a name-by-name lookup would raise and let
        // the item be marked AMBIGUOUS. The trade is worth it — the helper exists precisely to
        // check uniqueness within an experiment graph, a duplicate there is an abnormal state of
        // the instance, and the CSV importer makes the same choice — but nothing in this method
        // can detect the case, so it is stated rather than half-handled.
    }

    //#endregion

    //#region helpers

    private void applyMatches(ResolvedItem item, List<ResourceReference> matches,
                              ReportMessage missingHint) {
        if (matches.isEmpty()) {
            item.setStatus(ResolutionStatus.MISSING).setHint(missingHint);
        } else if (matches.size() == 1) {
            item.setStatus(ResolutionStatus.FOUND).setMatches(matches);
        } else {
            item.setStatus(ResolutionStatus.AMBIGUOUS).setMatches(matches)
                    .setHint(ReportMessage.of(HINT + "ambiguous",
                            "Several resources carry this name, so a human has to pick one."));
        }
    }

    private ResourceReference reference(SPARQLNamedResourceModel<?> model) {
        return new ResourceReference(model.getUri(), model.getName());
    }

    private boolean containsUri(List<ResourceReference> references, URI uri) {
        for (ResourceReference reference : references) {
            if (reference.getUri() != null && reference.getUri().equals(uri)) {
                return true;
            }
        }
        return false;
    }

    private List<String> distinct(String... values) {
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isEmpty() && !result.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }

    private String summarise(List<String> values) {
        int shown = Math.min(3, values.size());
        String head = String.join(", ", values.subList(0, shown));
        return values.size() > shown ? head + ", … (" + values.size() + " in all)" : head;
    }

    //#endregion
}
