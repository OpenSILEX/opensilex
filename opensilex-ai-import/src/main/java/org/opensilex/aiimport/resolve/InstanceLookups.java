//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.resolve;

import org.opensilex.aiimport.profile.PersonCandidate;
import org.opensilex.aiimport.workbook.HeaderMatcher;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.dal.ExperimentSearchFilter;
import org.opensilex.core.germplasm.api.GermplasmSearchFilter;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.organisation.bll.FacilityLogic;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.facility.FacilitySearchFilter;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.OrganizationSearchFilter;
import org.opensilex.core.project.dal.ProjectDAO;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.core.variable.dal.VariableSearchFilter;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.person.dal.PersonDAO;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Everything the resolver asks of the instance, in one place, always under the current user's
 * rights.
 * <p>
 * A gateway: {@link ResolutionService} decides <em>in which order</em> names are tried and what the
 * report says; this class only knows <em>how</em> each kind of resource is fetched through the
 * platform's own DAOs and logic classes. Nothing here writes, nothing here builds a report line,
 * and nothing here queries around an access check — a facility goes through {@code FacilityLogic},
 * a germplasm through the DAO that filters on the user's groups.
 * <p>
 * Three questions per kind of resource, which are the three steps of the resolution:
 * <ul>
 *     <li>which resources carry exactly this name ({@code …Named});</li>
 *     <li>is this URI still there, and may this user see it ({@link #visible}), for what the
 *     instance was taught;</li>
 *     <li>which resources could a misspelt name have meant ({@link #candidates},
 *     {@link #germplasmCloseTo}).</li>
 * </ul>
 *
 * @author Arnaud Charleroy
 */
class InstanceLookups {

    /**
     * How many resources one exact lookup brings back. A name is searched as a pattern, so it can
     * match its neighbours too; they are filtered out afterwards.
     */
    static final int EXACT_SEARCH_LIMIT = 10;

    /**
     * How many resources of one kind are compared against the misspelt names of that kind. Enough
     * for the experiments, projects, variables, facilities and people of any instance seen so far;
     * reaching it is reported, never passed over.
     */
    static final int NEAR_MATCH_CANDIDATE_LIMIT = 2000;

    /**
     * Germplasm can run to tens of thousands, so it is not listed but searched per name, through
     * the fragments the name shares with its correct spelling. This bounds each of those searches.
     */
    static final int GERMPLASM_NEAR_MATCH_LIMIT = 50;

    private final SPARQLService sparql;
    private final MongoDBService nosql;
    private final FileStorageService fs;
    private final AccountModel currentUser;

    InstanceLookups(SPARQLService sparql, MongoDBService nosql, FileStorageService fs,
                    AccountModel currentUser) {
        this.sparql = sparql;
        this.nosql = nosql;
        this.fs = fs;
        this.currentUser = currentUser;
    }

    //#region exact

    /**
     * By name or by acronym: a trial often names its project by the short name.
     */
    List<ResourceReference> projectsNamed(String name) throws Exception {
        List<ResourceReference> exact = new ArrayList<>();
        for (ProjectModel project : new ProjectDAO(sparql)
                .search(name, null, null, null, currentUser, null, 0, EXACT_SEARCH_LIMIT)
                .getList()) {
            if (name.equalsIgnoreCase(project.getName())
                    || name.equalsIgnoreCase(project.getShortname())) {
                exact.add(reference(project));
            }
        }
        return exact;
    }

    /**
     * Among the organisations this user may see, as the organisations screen lists them.
     */
    List<ResourceReference> organizationsNamed(String name) throws Exception {
        List<ResourceReference> exact = new ArrayList<>();
        for (OrganizationModel organization : new OrganizationDAO(sparql).search(
                new OrganizationSearchFilter().setNameFilter(name).setUser(currentUser))) {
            if (name.equalsIgnoreCase(organization.getName())) {
                exact.add(reference(organization));
            }
        }
        return exact;
    }

    /**
     * By name or by synonym. A variety is often written under one of its synonyms — "Pinot Grigio"
     * for Pinot gris. The DAO's name filter already searches them; the check has to accept them
     * too, or a variety known here is reported missing.
     */
    List<ResourceReference> germplasmNamed(String name) throws Exception {
        List<ResourceReference> exact = new ArrayList<>();
        for (GermplasmModel germplasm : new GermplasmDAO(sparql, nosql)
                .search(germplasmFilter(name, EXACT_SEARCH_LIMIT), false, false).getList()) {
            if (name.equalsIgnoreCase(germplasm.getName()) || isSynonymOf(name, germplasm)) {
                exact.add(reference(germplasm));
            }
        }
        return exact;
    }

    /**
     * Through {@code FacilityLogic} rather than a direct query: it computes the organisations and
     * sites the account may see and filters on them.
     */
    List<ResourceReference> facilitiesNamed(String name) throws Exception {
        FacilitySearchFilter filter = new FacilitySearchFilter()
                .setUser(currentUser)
                .setPattern(name);
        filter.setLang(currentUser.getLanguage());
        filter.setPage(0);
        filter.setPageSize(EXACT_SEARCH_LIMIT);

        List<ResourceReference> exact = new ArrayList<>();
        for (FacilityModel facility : new FacilityLogic(sparql, nosql, currentUser, fs)
                .search(filter).getList()) {
            // The pattern is a regex, so it returns near misses too.
            if (name.equalsIgnoreCase(facility.getName())) {
                exact.add(reference(facility));
            }
        }
        return exact;
    }

    /**
     * By the best key the candidate has: an ORCID identifies one human worldwide, an email one
     * mailbox, a name neither. Two agronomists called Martin are not the same person, so several
     * matches on a name come back as several, for the report to call ambiguous.
     */
    List<ResourceReference> personsMatching(PersonCandidate candidate) throws Exception {
        List<ResourceReference> matches = new ArrayList<>();
        String key = candidate.getSearchKey();
        if (key == null || key.isEmpty()) {
            return matches;
        }
        // The DAO's pattern runs over the given name, family name, email and ORCID at once, so one
        // query covers whichever key the file supplied.
        for (PersonModel person : new PersonDAO(sparql)
                .search(escapeRegex(key), null, 0, EXACT_SEARCH_LIMIT).getList()) {
            if (isTheSamePerson(person, candidate)) {
                matches.add(new ResourceReference(person.getUri(), fullNameOf(person)));
            }
        }
        return matches;
    }

    //#endregion

    //#region by URI

    /**
     * Reads a resource again by its URI, under the current user's rights, with its current name.
     * <p>
     * This is what stops a correction taught by one person from revealing to another a resource
     * they are not allowed to see: the correction names a URI, and only the platform's own access
     * checks decide whether this user gets it.
     *
     * @return the resource as this user may see it, empty when they may not or when it is gone
     */
    Optional<ResourceReference> visible(ReportCategory category, URI uri) throws Exception {
        switch (category) {
            case EXPERIMENTS:
                return experiment(uri).map(this::reference);
            case PROJECTS:
                return Optional.ofNullable(new ProjectDAO(sparql).get(uri, currentUser))
                        .map(this::reference);
            case VARIABLES:
                return Optional.ofNullable(new VariableDAO(sparql, nosql, fs, currentUser).get(uri))
                        .map(this::variableReference);
            case GERMPLASM:
                return Optional.ofNullable(new GermplasmDAO(sparql, nosql).get(uri, currentUser, false))
                        .map(this::reference);
            case FACILITIES:
                return Optional.ofNullable(new FacilityLogic(sparql, nosql, currentUser, fs)
                                .get(uri, currentUser))
                        .map(this::reference);
            case PERSONS:
                return Optional.ofNullable(new PersonDAO(sparql).get(uri))
                        .map(person -> new ResourceReference(person.getUri(), fullNameOf(person)));
            case ORGANIZATIONS:
                return Optional.ofNullable(new OrganizationDAO(sparql).get(uri, currentUser))
                        .map(this::reference);
            default:
                return Optional.empty();
        }
    }

    /**
     * The experiment itself rather than a reference to it: the plots are looked up inside it.
     */
    Optional<ExperimentModel> experiment(URI uri) throws Exception {
        return Optional.ofNullable(new ExperimentDAO(sparql, nosql, fs).get(uri, currentUser));
    }

    //#endregion

    //#region near-match candidates

    /**
     * The resources of one kind to compare misspelt names against, up to
     * {@link #NEAR_MATCH_CANDIDATE_LIMIT}. Germplasm is not listed — see {@link #germplasmCloseTo}.
     * <p>
     * A resource with a second name (a project's acronym, a variable's alternative name) is listed
     * under both, so a misspelt acronym is recognised too.
     */
    List<ResourceReference> candidates(ReportCategory category) throws Exception {
        List<ResourceReference> candidates = new ArrayList<>();
        switch (category) {
            case EXPERIMENTS: {
                ExperimentSearchFilter filter = new ExperimentSearchFilter();
                filter.setUser(currentUser);
                filter.setPage(0);
                filter.setPageSize(NEAR_MATCH_CANDIDATE_LIMIT);
                for (ExperimentModel experiment : new ExperimentDAO(sparql, nosql, fs)
                        .search(filter, false, false, false).getList()) {
                    candidates.add(reference(experiment));
                }
                break;
            }
            case PROJECTS:
                for (ProjectModel project : new ProjectDAO(sparql)
                        .search(null, null, null, null, currentUser, null, 0, NEAR_MATCH_CANDIDATE_LIMIT)
                        .getList()) {
                    candidates.add(reference(project));
                    addAlias(candidates, project.getUri(), project.getShortname());
                }
                break;
            case VARIABLES: {
                VariableSearchFilter filter = new VariableSearchFilter().setUserModel(currentUser);
                filter.setLang(currentUser.getLanguage());
                filter.setPage(0);
                filter.setPageSize(NEAR_MATCH_CANDIDATE_LIMIT);
                for (VariableModel variable : new VariableDAO(sparql, nosql, fs, currentUser)
                        .search(filter).getList()) {
                    candidates.add(reference(variable));
                    addAlias(candidates, variable.getUri(), variable.getAlternativeName());
                }
                break;
            }
            case FACILITIES: {
                // Through the facility logic, for the same reason as the exact lookup: a suggestion
                // must never reveal a facility the user is not entitled to see.
                FacilitySearchFilter filter = new FacilitySearchFilter().setUser(currentUser);
                filter.setLang(currentUser.getLanguage());
                filter.setPage(0);
                filter.setPageSize(NEAR_MATCH_CANDIDATE_LIMIT);
                for (FacilityModel facility : new FacilityLogic(sparql, nosql, currentUser, fs)
                        .search(filter).getList()) {
                    candidates.add(reference(facility));
                }
                break;
            }
            case PERSONS:
                for (PersonModel person : new PersonDAO(sparql)
                        .search(".*", null, 0, NEAR_MATCH_CANDIDATE_LIMIT).getList()) {
                    candidates.add(new ResourceReference(person.getUri(), fullNameOf(person)));
                }
                break;
            case ORGANIZATIONS:
                new OrganizationDAO(sparql).search(new OrganizationSearchFilter().setUser(currentUser)).stream()
                        .limit(NEAR_MATCH_CANDIDATE_LIMIT)
                        .forEach(organization -> candidates.add(reference(organization)));
                break;
            default:
                break;
        }
        return candidates;
    }

    /**
     * Germplasm is searched per name rather than listed: an instance can hold tens of thousands,
     * and the name's three-letter fragments find its correct spelling among them — a typo spoils
     * one or two fragments, never all of them.
     *
     * @return the germplasm sharing a fragment with the name; empty for a name too short to be
     * recognised by its spelling
     */
    List<ResourceReference> germplasmCloseTo(String name) throws Exception {
        List<ResourceReference> candidates = new ArrayList<>();
        String fragments = fragmentPattern(name);
        if (fragments == null) {
            return candidates;
        }
        for (GermplasmModel germplasm : new GermplasmDAO(sparql, nosql)
                .search(germplasmFilter(fragments, GERMPLASM_NEAR_MATCH_LIMIT), false, false)
                .getList()) {
            candidates.add(reference(germplasm));
        }
        return candidates;
    }

    //#endregion

    //#region SPARQL patterns

    /**
     * An alternation of the name's three-letter fragments, escaped: any stored name sharing one of
     * them is a candidate. Null when the name is too short to be recognised by spelling at all.
     */
    static String fragmentPattern(String name) {
        String normalised = HeaderMatcher.normalize(name);
        if (normalised.length() <= NameSimilarity.EXACT_ONLY_UP_TO) {
            return null;
        }
        Set<String> fragments = new LinkedHashSet<>();
        for (int i = 0; i + 3 <= normalised.length(); i++) {
            fragments.add(escapeRegex(normalised.substring(i, i + 3)));
        }
        return String.join("|", fragments);
    }

    /**
     * Escapes a literal for a SPARQL {@code REGEX}.
     * <p>
     * Character by character rather than {@code Pattern.quote}: the {@code \Q…\E} form is a Java
     * extension, not part of the XPath regular expressions SPARQL specifies, so it works on one
     * triplestore and silently matches nothing on another.
     */
    static String escapeRegex(String literal) {
        StringBuilder escaped = new StringBuilder(literal.length() + 8);
        for (char c : literal.toCharArray()) {
            if ("\\.^$|?*+()[]{}-".indexOf(c) >= 0) {
                escaped.append('\\');
            }
            escaped.append(c);
        }
        return escaped.toString();
    }

    //#endregion

    //#region helpers

    ResourceReference reference(SPARQLNamedResourceModel<?> model) {
        return new ResourceReference(model.getUri(), model.getName());
    }

    /**
     * A variable with its datatype, which the type check of the observed values needs.
     */
    ResourceReference variableReference(VariableModel variable) {
        return reference(variable).setDatatype(variable.getDataType() == null
                ? null
                : variable.getDataType().toString());
    }

    private GermplasmSearchFilter germplasmFilter(String name, int pageSize) {
        GermplasmSearchFilter filter = new GermplasmSearchFilter();
        filter.setName(name);
        filter.setUser(currentUser);
        filter.setLang(currentUser.getLanguage());
        filter.setPage(0);
        filter.setPageSize(pageSize);
        return filter;
    }

    private boolean isSynonymOf(String name, GermplasmModel germplasm) {
        return germplasm.getSynonyms() != null
                && germplasm.getSynonyms().stream().anyMatch(name::equalsIgnoreCase);
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

    private void addAlias(List<ResourceReference> candidates, URI uri, String alias) {
        if (alias != null && !alias.isEmpty()) {
            candidates.add(new ResourceReference(uri, alias));
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    //#endregion
}
