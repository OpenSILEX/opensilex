//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.resolve;

import org.opensilex.aiimport.profile.ExtractedImportPlan;
import org.opensilex.aiimport.profile.PersonCandidate;
import org.opensilex.aiimport.profile.VariableCandidate;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.service.SPARQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Checks, against this instance, every name a profile read out of an uploaded file.
 * <p>
 * The whole point of this class is that no URI ever originates from the language model: the
 * assistant is handed the report this service produces, and can only cite what a database returned.
 * <p>
 * Each name goes down the same chain, in order of confidence, and stops at the first step that
 * settles it:
 * <ol>
 *     <li><b>confirmed</b> — what the user has already said this spelling means, in this
 *     conversation;</li>
 *     <li><b>exact</b> — a resource carrying exactly this name;</li>
 *     <li><b>learned</b> — a misspelling the instance was taught, re-read under this user's
 *     rights;</li>
 *     <li><b>near</b> — names merely close, only ever suggested, never resolved.</li>
 * </ol>
 * This class holds the chain and writes the report. How each kind of resource is fetched lives in
 * {@link InstanceLookups}; the variables, which are tried in more ways than the rest, in
 * {@link VariableResolver}.
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

    private static final Logger LOGGER = LoggerFactory.getLogger(ResolutionService.class);

    private final SPARQLService sparql;
    private final MongoDBService nosql;
    private final FileStorageService fs;
    private final AccountModel currentUser;
    private final InstanceLookups lookups;
    private final VariableResolver variables;

    /**
     * Set for the duration of one {@link #resolve} call. The service is built per analysis, so this
     * is state of that analysis, not of the service.
     */
    private ConfirmedMatches confirmed = new ConfirmedMatches();

    /**
     * The misspellings the instance was taught, read once per analysis.
     */
    private CorrectionStore.Corrections learned = new CorrectionStore.Corrections();

    public ResolutionService(SPARQLService sparql,
                             MongoDBService nosql,
                             FileStorageService fs,
                             AccountModel currentUser,
                             SharedResourceVariableLookup sharedResources) {
        this.sparql = sparql;
        this.nosql = nosql;
        this.fs = fs;
        this.currentUser = currentUser;
        this.lookups = new InstanceLookups(sparql, nosql, fs, currentUser);
        this.variables = new VariableResolver(sparql, nosql, fs, currentUser, sharedResources, lookups);
    }

    public ResolutionReport resolve(ExtractedImportPlan plan) {
        return resolve(plan, new ConfirmedMatches());
    }

    /**
     * @param confirmed what the user has already said a misspelt name means; consulted before any
     *                  search, so a confirmed name resolves exactly as a correctly spelt one would
     */
    public ResolutionReport resolve(ExtractedImportPlan plan, ConfirmedMatches confirmed) {
        this.confirmed = confirmed == null ? new ConfirmedMatches() : confirmed;
        ResolutionReport report = new ResolutionReport().setProfileId(plan.getProfileId());
        report.addAnomalies(plan.getAnomalyMessages());
        report.getNotes().putAll(plan.getNotes());

        this.learned = loadCorrections(report);

        // Confirmed, then exact, for every category.
        List<ExperimentModel> resolvedExperiments = resolveExperiments(plan, report);
        // Before the plots: they are looked up inside the experiment, however it was recognised.
        resolvedExperiments.addAll(applyLearnedExperiments(report));
        resolveEach(report, ReportCategory.PROJECTS, plan.getProjectNames(), ResolvedItem::new,
                lookups::projectsNamed, "project",
                ReportMessage.of(HINT + "projectMissing",
                        "No project carries this name. Ask the user which project the trial "
                                + "belongs to, or create it."));
        resolveVariables(plan, report);
        resolveEach(report, ReportCategory.GERMPLASM, plan.getGermplasmNames(), ResolvedItem::new,
                lookups::germplasmNamed, "germplasm",
                ReportMessage.of(HINT + "germplasmMissing",
                        "No germplasm carries this name. Check the naming convention with the "
                                + "user before creating anything."));
        resolveEach(report, ReportCategory.PERSONS, plan.getPersons(),
                candidate -> new ResolvedItem(candidate.getName()).setExternalId(candidate.getOrcid()),
                lookups::personsMatching, "person",
                ReportMessage.of(HINT + "personMissing",
                        "Nobody here matches this person. Create them, or say which existing "
                                + "person they are."));
        resolveEach(report, ReportCategory.ORGANIZATIONS, plan.getOrganizationNames(),
                name -> new ResolvedItem(name).setParentValue(plan.getOrganizationParents().get(name)),
                lookups::organizationsNamed, "organization",
                ReportMessage.of(HINT + "organizationMissing",
                        "No organisation carries this name. Create it — a unit as part of its "
                                + "institution — or say which existing organisation it is."));
        resolveScientificObjects(plan, report, resolvedExperiments);
        // A field named in the file is a place, and places outlive experiments — so it is looked
        // up across the instance rather than inside one experiment's graph.
        resolveEach(report, ReportCategory.FACILITIES, plan.getFacilityNames(),
                name -> new ResolvedItem(name).setDetails(plan.getFacilityDetails().get(name)),
                lookups::facilitiesNamed, "facility",
                ReportMessage.of(HINT + "facilityMissing",
                        "No facility carries this name. Create it with the Create button beside it, "
                                + "which opens the platform's form with what the file says of it, "
                                + "or tell the assistant which existing one the field corresponds "
                                + "to."));

        // Then what is still missing, in order of confidence: a misspelling the instance was taught
        // resolves outright; a name merely close is only suggested.
        applyLearnedCorrections(report);
        suggestNearMatches(report);

        return report;
    }

    /**
     * A resource as this user may see it, with its current name; empty when they may not or when it
     * does not exist. What the API checks before binding a row of the file to a resource the user
     * has just created: the URI comes from the platform's creation response, and is read back here
     * under the user's rights rather than trusted.
     */
    public Optional<ResourceReference> visibleResource(ReportCategory category, URI uri) {
        return visible(category, uri);
    }

    //#region confirmed and exact

    /**
     * An exact lookup of one thing the file names — a name, or a person with their identifiers.
     */
    @FunctionalInterface
    interface ExactLookup<S> {
        List<ResourceReference> find(S source) throws Exception;
    }

    /**
     * The first two steps of the chain for a category whose resources are recognised by a name
     * alone: a confirmation if the user gave one, otherwise the exact lookup.
     * <p>
     * A lookup that fails is a warning on the report, never an exception: one unreachable DAO must
     * not cost the user the rest of the analysis.
     *
     * @param newItem     the report line for one source, before it is resolved
     * @param noun        what the category is called in a warning, "project", "person"…
     * @param missingHint what the report says when nothing carries the name
     */
    private <S> void resolveEach(ResolutionReport report, ReportCategory category, List<S> sources,
                                 Function<S, ResolvedItem> newItem, ExactLookup<S> lookup,
                                 String noun, ReportMessage missingHint) {
        List<ResolvedItem> items = category.itemsOf(report);
        for (S source : sources) {
            ResolvedItem item = newItem.apply(source);
            items.add(item);
            if (confirmed.resolve(category, item)) {
                continue;
            }
            try {
                applyMatches(item, lookup.find(source), missingHint);
            } catch (Exception e) {
                String name = item.getSourceValue();
                LOGGER.warn("Could not resolve {} {}", noun, name, e);
                report.addWarning(ReportMessage.of(WARN + noun + "LookupFailed",
                        "The " + noun + " '" + name + "' could not be looked up.").with("name", name));
            }
        }
    }

    private List<ExperimentModel> resolveExperiments(ExtractedImportPlan plan, ResolutionReport report) {
        List<ExperimentModel> resolved = new ArrayList<>();
        ExperimentDAO dao = new ExperimentDAO(sparql, nosql, fs);

        for (String name : plan.getExperimentNames()) {
            ResolvedItem item = new ResolvedItem(name);
            report.getExperiments().add(item);
            if (confirmed.resolve(ReportCategory.EXPERIMENTS, item)) {
                // The plots are looked up inside the experiment, so it has to be loaded as well.
                loadExperiment(item.getMatches().get(0).getUri(), name).ifPresent(resolved::add);
                continue;
            }
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
                            .getMatches().add(lookups.reference(experiment));
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

    private void resolveVariables(ExtractedImportPlan plan, ResolutionReport report) {
        for (VariableCandidate candidate : plan.getVariables()) {
            ResolvedItem item = new ResolvedItem(candidate.getColumnKey())
                    .setExternalId(candidate.getExternalId());
            report.getVariables().add(item);
            if (confirmed.resolve(ReportCategory.VARIABLES, item)) {
                // The type check needs the datatype, which a confirmation does not carry.
                variables.fetchDatatypes(item.getMatches(), report);
                continue;
            }
            variables.resolve(candidate, item, report);
        }

        if (!plan.getVariables().isEmpty()) {
            report.getNotes().put("variable columns", String.valueOf(plan.getVariables().size()));
        }
    }

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

    //#region learned corrections

    private CorrectionStore.Corrections loadCorrections(ResolutionReport report) {
        try {
            return new CorrectionStore(sparql, CorrectionStore.graphFor(sparql.getBaseURI())).load();
        } catch (Exception e) {
            LOGGER.warn("Could not read the corrections taught to this instance", e);
            report.addWarning(ReportMessage.of(WARN + "correctionsUnreadable",
                    "The corrections this instance was taught could not be read, so misspellings "
                            + "it knows are treated as unknown this time."));
            return new CorrectionStore.Corrections();
        }
    }

    /**
     * Experiments first and apart, because the plots are resolved inside them and need the model.
     */
    private List<ExperimentModel> applyLearnedExperiments(ResolutionReport report) {
        List<ExperimentModel> resolved = new ArrayList<>();
        for (ResolvedItem item : missingLearnable(report, ReportCategory.EXPERIMENTS)) {
            LearnedCorrection correction =
                    learned.lookup(ReportCategory.EXPERIMENTS, item.getSourceValue()).get();
            loadExperiment(correction.getTarget(), item.getSourceValue()).ifPresent(experiment -> {
                applyLearned(item, correction, lookups.reference(experiment));
                resolved.add(experiment);
            });
        }
        return resolved;
    }

    private void applyLearnedCorrections(ResolutionReport report) {
        for (ReportCategory category : ReportCategory.values()) {
            if (category == ReportCategory.EXPERIMENTS) {
                continue;
            }
            for (ResolvedItem item : missingLearnable(report, category)) {
                LearnedCorrection correction = learned.lookup(category, item.getSourceValue()).get();
                visible(category, correction.getTarget())
                        .ifPresent(current -> applyLearned(item, correction, current));
            }
        }
    }

    /**
     * Missing items of a category for which a correction was taught. Only missing ones: a name
     * spelt exactly as a resource here always wins over what was learned about it.
     */
    private List<ResolvedItem> missingLearnable(ResolutionReport report, ReportCategory category) {
        List<ResolvedItem> items = new ArrayList<>();
        if (learned.isEmpty()) {
            return items;
        }
        for (ResolvedItem item : missingItems(report, category)) {
            if (learned.lookup(category, item.getSourceValue()).isPresent()) {
                items.add(item);
            }
        }
        return items;
    }

    private void applyLearned(ResolvedItem item, LearnedCorrection correction,
                              ResourceReference current) {
        String author = correction.getAuthor() == null ? "" : correction.getAuthor();
        String date = correction.getCreated() == null
                ? ""
                : correction.getCreated().toLocalDate().toString();
        item.setStatus(ResolutionStatus.FOUND)
                .setLearnedCorrection(correction)
                .setMatches(new ArrayList<>(List.of(current)))
                .setHint(ReportMessage.of(HINT + "learnedCorrection",
                                "Recognised as '" + current.getName() + "' from a correction "
                                        + (author.isEmpty() ? "" : "by " + author + " ")
                                        + (date.isEmpty() ? "" : "on " + date + " ")
                                        + "that this instance remembers.")
                        .with("name", current.getName())
                        .with("author", author)
                        .with("date", date));
    }

    /**
     * The resource a correction names, as this user may see it. Not visible, or gone: the
     * correction simply does not apply to them.
     */
    private Optional<ResourceReference> visible(ReportCategory category, URI uri) {
        try {
            return lookups.visible(category, uri);
        } catch (Exception e) {
            LOGGER.debug("{} {} not visible to this user", category.getKey(), uri, e);
            return Optional.empty();
        }
    }

    private Optional<ExperimentModel> loadExperiment(URI uri, String sourceValue) {
        try {
            return lookups.experiment(uri);
        } catch (Exception e) {
            LOGGER.debug("Experiment {} for {} not visible to this user", uri, sourceValue, e);
            return Optional.empty();
        }
    }

    //#endregion

    //#region near matches

    /**
     * A source of resources to compare misspelt names against. Called at most once per category
     * and per analysis, and only when that category has something missing.
     */
    @FunctionalInterface
    interface CandidateSource {
        List<ResourceReference> list() throws Exception;
    }

    private void suggestNearMatches(ResolutionReport report) {
        for (ReportCategory category : List.of(ReportCategory.EXPERIMENTS, ReportCategory.PROJECTS,
                ReportCategory.VARIABLES, ReportCategory.FACILITIES, ReportCategory.PERSONS,
                ReportCategory.ORGANIZATIONS)) {
            suggest(report, category, () -> lookups.candidates(category));
        }
        suggestGermplasm(report);
    }

    private void suggest(ResolutionReport report, ReportCategory category, CandidateSource source) {
        List<ResolvedItem> missing = missingItems(report, category);
        if (missing.isEmpty()) {
            return;
        }
        List<ResourceReference> candidates;
        try {
            candidates = source.list();
        } catch (Exception e) {
            LOGGER.warn("Could not list the {} to compare against", category.getKey(), e);
            report.addWarning(ReportMessage.of(WARN + "nearMatchLookupFailed",
                    "Names close to the missing " + category.getKey() + " could not be looked "
                            + "for.").with("category", category.getKey()));
            return;
        }
        int limit = InstanceLookups.NEAR_MATCH_CANDIDATE_LIMIT;
        if (candidates.size() >= limit) {
            // Said out loud: a suggestion missing because of a limit looks exactly like a name
            // with no close match, and the user deserves to know which one it is.
            report.addWarning(ReportMessage.of(WARN + "nearMatchLimitReached",
                            "Only the first " + limit + " " + category.getKey()
                                    + " were compared against the missing names, so a close match "
                                    + "may have been missed.")
                    .with("category", category.getKey())
                    .with("limit", limit));
        }
        NearMatchFinder finder = new NearMatchFinder();
        for (ResolvedItem item : missing) {
            attachSuggestions(item, finder.suggest(item.getSourceValue(), candidates));
        }
    }

    /**
     * Germplasm is compared name by name, against the few resources sharing a fragment with it,
     * rather than against a list of all of them.
     */
    private void suggestGermplasm(ResolutionReport report) {
        NearMatchFinder finder = new NearMatchFinder();
        for (ResolvedItem item : missingItems(report, ReportCategory.GERMPLASM)) {
            try {
                attachSuggestions(item, finder.suggest(item.getSourceValue(),
                        lookups.germplasmCloseTo(item.getSourceValue())));
            } catch (Exception e) {
                LOGGER.warn("Could not look for germplasm close to {}", item.getSourceValue(), e);
            }
        }
    }

    private void attachSuggestions(ResolvedItem item, List<ResourceReference> suggestions) {
        if (suggestions.isEmpty()) {
            return;
        }
        item.getSuggestions().addAll(suggestions);
        String closest = suggestions.get(0).getName();
        item.setHint(ReportMessage.of(HINT + "didYouMean",
                        "Not found as written. Did you mean '" + closest + "'? Confirm it rather "
                                + "than creating a second one under this spelling.")
                .with("name", closest));
    }

    private List<ResolvedItem> missingItems(ResolutionReport report, ReportCategory category) {
        List<ResolvedItem> missing = new ArrayList<>();
        if (!category.allowsNearMatching()) {
            return missing;
        }
        for (ResolvedItem item : category.itemsOf(report)) {
            if (item.getStatus() == ResolutionStatus.MISSING) {
                missing.add(item);
            }
        }
        return missing;
    }

    //#endregion

    //#region helpers

    /**
     * One match is found, several are for a human to pick from, none is missing.
     */
    static void applyMatches(ResolvedItem item, List<ResourceReference> matches,
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

    private String summarise(List<String> values) {
        int shown = Math.min(3, values.size());
        String head = String.join(", ", values.subList(0, shown));
        return values.size() > shown ? head + ", … (" + values.size() + " in all)" : head;
    }

    //#endregion
}
