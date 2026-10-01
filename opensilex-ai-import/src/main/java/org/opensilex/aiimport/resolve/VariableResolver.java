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
import org.opensilex.aiimport.profile.VariableCandidate;
import org.opensilex.aiimport.profile.VariableComponent;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.variable.dal.BaseVariableDAO;
import org.opensilex.core.variable.dal.BaseVariableModel;
import org.opensilex.core.variable.dal.CharacteristicModel;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.core.variable.dal.UnitModel;
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
import java.util.List;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * Resolves one variable column of the file, the richest of the categories.
 * <p>
 * Where a project or a facility is simply found or not, a variable is tried in order of
 * reliability — the ontology identifier the file carries, then its name, then a shared resource
 * instance — and, when it is nowhere, the four components it would be created from are resolved in
 * its place, so that the creation form reuses what the instance already has.
 * <p>
 * Split from {@link ResolutionService} because none of this concerns the other categories; the
 * service still decides when it runs (after a confirmation has been looked for) and what comes
 * after (learned corrections, near matches).
 *
 * @author Arnaud Charleroy
 */
class VariableResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(VariableResolver.class);

    private static final String HINT = "AiImport.report.hint.";
    private static final String WARN = "AiImport.report.warning.";

    /**
     * How many candidates a component lookup brings back. The search runs on the name's fragments,
     * so it returns the exact spelling and its neighbours together; fifty leaves room for both.
     */
    private static final int COMPONENT_SEARCH_LIMIT = 50;

    private final SPARQLService sparql;
    private final AccountModel currentUser;
    private final SharedResourceVariableLookup sharedResources;
    private final InstanceLookups lookups;
    private final VariableDAO dao;

    VariableResolver(SPARQLService sparql, MongoDBService nosql, FileStorageService fs,
                     AccountModel currentUser, SharedResourceVariableLookup sharedResources,
                     InstanceLookups lookups) {
        this.sparql = sparql;
        this.currentUser = currentUser;
        this.sharedResources = sharedResources;
        this.lookups = lookups;
        this.dao = new VariableDAO(sparql, nosql, fs, currentUser);
    }

    /**
     * Settles the item: found here, found on a shared resource instance, or missing with its
     * components resolved.
     */
    void resolve(VariableCandidate candidate, ResolvedItem item, ResolutionReport report) {
        List<ResourceReference> matches = new ArrayList<>();

        // The ontology identifier the file carries is the most reliable key, so try it first.
        if (candidate.getExternalId() != null && !candidate.getExternalId().isEmpty()) {
            matches.addAll(searchByExactMatch(candidate.getExternalId(), report));
        }
        if (!matches.isEmpty()) {
            // The ontology query returns uri and name only; the datatype comes from the model.
            fetchDatatypes(matches, report);
        } else {
            matches.addAll(searchByName(candidate, report));
        }

        if (!matches.isEmpty()) {
            ResolutionService.applyMatches(item, matches, null);
            return;
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
            return;
        }

        // Nowhere to be found, so the next step is creating it — and that takes four components.
        // Whatever the file says about them is resolved now: a component that already exists here
        // must be reused, never entered a second time.
        resolveComponents(candidate, item);

        item.setStatus(ResolutionStatus.MISSING)
                .setHint(missingVariableHint(candidate));
    }

    /**
     * Fills in the datatype of variables matched without it — by their ontology identifier, or by
     * a confirmation, which carries a URI and a name only.
     */
    void fetchDatatypes(List<ResourceReference> matches, ResolutionReport report) {
        List<URI> uris = new ArrayList<>(matches.size());
        matches.forEach(match -> uris.add(match.getUri()));
        try {
            for (VariableModel variable : dao.getList(uris, currentUser.getLanguage())) {
                for (ResourceReference match : matches) {
                    if (match.getUri() != null && match.getUri().equals(variable.getUri())) {
                        match.setDatatype(lookups.variableReference(variable).getDatatype());
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

    //#region lookups

    private List<ResourceReference> searchByName(VariableCandidate candidate,
                                                 ResolutionReport report) {
        List<ResourceReference> matches = new ArrayList<>();
        for (String name : distinct(candidate.getColumnKey(), candidate.getLabel())) {
            try {
                VariableSearchFilter filter = new VariableSearchFilter()
                        .setNamePattern(name)
                        .setUserModel(currentUser);
                filter.setLang(currentUser.getLanguage());
                filter.setPage(0);
                filter.setPageSize(InstanceLookups.EXACT_SEARCH_LIMIT);

                for (VariableModel variable : dao.search(filter).getList()) {
                    if (isExactVariableMatch(name, variable)
                            && !containsUri(matches, variable.getUri())) {
                        matches.add(lookups.variableReference(variable));
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
            select.setLimit(InstanceLookups.EXACT_SEARCH_LIMIT);

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

    //#endregion

    //#region components

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
        // One search serves both answers: the name's fragments bring back its exact spelling when
        // it exists, and its near spellings when it does not. A name too short for fragments is
        // searched as written, and can only match exactly.
        String fragments = InstanceLookups.fragmentPattern(component.getName());
        String pattern = fragments != null
                ? fragments
                : InstanceLookups.escapeRegex(component.getName());
        try {
            List<ResourceReference> candidates = new ArrayList<>();
            for (T model : new BaseVariableDAO<>(modelClass, sparql)
                    .search(pattern, null, 0, COMPONENT_SEARCH_LIMIT, currentUser.getLanguage())
                    .getList()) {
                if (component.getName().equalsIgnoreCase(model.getName())) {
                    resolved.setUri(model.getUri());
                    return;
                }
                candidates.add(new ResourceReference(model.getUri(), model.getName()));
            }
            // Shown next to the component, never selected in its place: the variable form's own
            // selector is where the user confirms which one they mean.
            new NearMatchFinder().suggest(component.getName(), candidates).stream()
                    .findFirst()
                    .ifPresent(resolved::setSuggestion);
        } catch (Exception e) {
            // Not worth a warning of its own: the variable is already reported missing, and an
            // unresolved component only means one more choice on the form.
            LOGGER.warn("Could not look up the {} '{}'", role, component.getName(), e);
        }
    }

    //#endregion

    //#region helpers

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

    //#endregion
}
