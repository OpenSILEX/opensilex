/*
 * *****************************************************************************
 *                         OrganizationDAO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 25/06/2024 10:10
 * Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr, gabriel.besombes@inrae.fr
 * *****************************************************************************
 */
package org.opensilex.core.organisation.dal;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.clauses.WhereClause;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_ZeroOrMore1;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ForbiddenURIAccessException;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.mapping.SPARQLListFetcher;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * @author vidalmor
 */
public class OrganizationDAO {

    protected final SPARQLService sparql;
    protected final OrganizationSPARQLHelper organizationSPARQLHelper;

    /**
     * Associates a user URI to the map of their accessible organizations. The keys of the map are the URIs of all
     * accessible organizations, and the values are the {@link OrganizationModel} corresponding to the URI. The models
     * are retrieved using a {@link SPARQLListFetcher} for the {@link OrganizationModel#children} field, so only the
     * URI of the children is accessible.
     */
    private static final Cache<URI, Map<URI, OrganizationModel>> userOrganizationCache = Caffeine.newBuilder()
            .build();

    public void invalidateCache() {
        userOrganizationCache.invalidateAll();
    }

    public OrganizationDAO(SPARQLService sparql) throws Exception {
        this.sparql = sparql;

        this.organizationSPARQLHelper = new OrganizationSPARQLHelper(sparql);
    }

    /**
     * Validates that the user has access to an organization. Throws an exception if that is not the case. The
     * organization must exist, and the user/organization must satisfy at least one of the following conditions :
     *
     * <ul>
     *     <li>The user is admin</li>
     *     <li>The user is a member of a group associated with the organization</li>
     *     <li>The user is a member of a group associated with a parent organization</li>
     *     <li>The user is the publisher of the organization</li>
     * </ul>
     *
     * @param organizationURI The organization URI to check
     * @param user The user
     * @throws IllegalArgumentException If the userModel is null
     */
    public void validateOrganizationAccess(URI organizationURI, AccountModel user) throws Exception {
        if (Objects.isNull(user)) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (!sparql.uriExists(OrganizationModel.class, organizationURI)) {
            throw new NotFoundURIException(organizationURI);
        }

        if (user.isAdmin()) {
            return;
        }

        Map<URI, OrganizationModel> userOrganizations = userOrganizationCache.getIfPresent(user.getUri());
        if (Objects.nonNull(userOrganizations)) {
            if (userOrganizations.containsKey(organizationURI)) {
                return;
            }
            throw new ForbiddenURIAccessException(organizationURI);
        }

        Var uriVar = makeVar(OrganizationModel.URI_FIELD);

        AskBuilder ask = new AskBuilder();
        ask.addWhere(uriVar, Ontology.typeSubClassAny, sparql.getRDFType(OrganizationModel.class));
        ask.addFilter(SPARQLQueryHelper.eq(uriVar, SPARQLDeserializers.nodeURI(organizationURI)));
        organizationSPARQLHelper.addOrganizationAccessClause(ask, uriVar, user.getUri());

        if (!sparql.executeAskQuery(ask)) {
            throw new ForbiddenURIAccessException(organizationURI);
        }
    }

    /**
     * MUST BE CALLED BEFORE AN UPDATE
     * <p>
     * Validate the organization hierarchy, i.e. fails if modifying the parents of the organization results in a
     * cycle in the hierarchy graph (for example, adding a child organization as a parent creates a cycle).
     * </p>
     *
     * @param model the organization to update
     * @throws BadRequestException if a cycle is found
     */
    protected void validateOrganizationHierarchy(OrganizationModel model) throws Exception {
        AskBuilder ask = sparql.getUriExistsQuery(OrganizationModel.class, model.getUri());

        Node nodeUri = SPARQLDeserializers.nodeURI(model.getUri());
        Var childrenVar = makeVar(OrganizationModel.CHILDREN_FIELD);

        ask.addWhere(nodeUri, new P_ZeroOrMore1(new P_Link(Oeso.hasPart.asNode())), childrenVar);

        // We must check if any of the parents of the organization is a child of itself
        // In that case this would create a cycle, thus we forbid it
        List<URI> parentUriList = model.getParents()
                .stream().map(OrganizationModel::getUri)
                .collect(Collectors.toList());

        if (parentUriList.isEmpty()) {
            // No parents to check, so no need to do a query
            return;
        }

        SPARQLQueryHelper.addWhereUriValues(ask, childrenVar.getVarName(), parentUriList);

        if (sparql.executeAskQuery(ask)) {
            throw new BadRequestException("Invalid organization model : parents cannot be children of the organization");
        }
    }

    /**
     * <p>
     * Retrieves all organizations the user can access. If there exists a cache entry for this user, the values from
     * the cache are returned. Otherwise, performs the SPARQL query and stores the results in the cache.
     * </p>
     * <p>
     * Note that children organizations are retrieved using {@link SPARQLListFetcher}, so only the URI field will be
     * accessible.
     * </p>
     *
     * @param user The user model
     * @return The list of organizations accessible to users
     */
    private List<OrganizationModel> searchWithoutFilters(AccountModel user) throws Exception {
        Map<URI, OrganizationModel> userOrganizations = userOrganizationCache.getIfPresent(user.getUri());
        if (Objects.nonNull(userOrganizations)) {
            return new ArrayList<>(userOrganizations.values());
        }

        AtomicReference<SelectBuilder> initialSelect = new AtomicReference<>();
        List<OrganizationModel> models;

        if (user.isAdmin()) {
            models = sparql.search(OrganizationModel.class, sparql.getDefaultLang(), initialSelect::set);
        } else {
            String lang = user.getLanguage();

            models = sparql.search(OrganizationModel.class, lang, selectBuilder -> {
                organizationSPARQLHelper.addOrganizationAccessClause(selectBuilder, makeVar(OrganizationModel.URI_FIELD), user.getUri());
                initialSelect.set(selectBuilder);
            });
        }

        SPARQLListFetcher<OrganizationModel> listFetcher = new SPARQLListFetcher<>(
                sparql,
                OrganizationModel.class,
                sparql.getDefaultGraph(OrganizationModel.class),
                Collections.singleton(OrganizationModel.CHILDREN_FIELD),
                models
        );
        listFetcher.updateModels();

        userOrganizationCache.put(user.getUri(), models.stream().collect(Collectors.toMap(
                OrganizationModel::getUri,
                Function.identity()
        )));

        return models;
    }

    /**
     * <p>
     * Get all organizations accessible by a giver user. See {@link OrganizationSPARQLHelper#addOrganizationAccessClause(WhereClause, Var, URI)}
     * for further information on the conditions for an organization to be considered accessible.
     * </p>
     * <p>
     * Note that children organizations are retrieved using {@link SPARQLListFetcher}, so only the URI field will be
     * accessible.
     * </p>
     *
     * @param filter an {@link OrganizationSearchFilter} object containing the filtering parameters
     * @return The accessible organizations
     */
    public List<OrganizationModel> search(OrganizationSearchFilter filter) throws Exception {
        if (Objects.isNull(filter.getUser())) {
            throw new IllegalArgumentException("User cannot be null");
        }

        Set<URI> restrictedOrganizationSet = Objects.nonNull(filter.getRestrictedOrganizations()) ?  new HashSet<>(filter.getRestrictedOrganizations()) : null;
        Pattern namePattern = StringUtils.isNotEmpty(filter.getNameFilter()) ? Pattern.compile(filter.getNameFilter(), Pattern.CASE_INSENSITIVE) : null;
        return searchWithoutFilters(filter.getUser()).stream().filter(org -> {
            if (Objects.nonNull(namePattern) && !namePattern.matcher(org.getName()).find()) {
                return false;
            }
            if (Objects.nonNull(filter.getTypeUriFilter()) && !SPARQLDeserializers.compareURIs(filter.getTypeUriFilter(),org.getType())) {
                return false;
            }
            if (Objects.nonNull(filter.getFacilityURI()) &&  org.getFacilities().stream().noneMatch(facilityModel -> SPARQLDeserializers.compareURIs(filter.getFacilityURI(),facilityModel.getUri()))) {
                return false;
            }
            if (Objects.nonNull(filter.getDirectChildURI()) &&  org.getChildren().stream().noneMatch(parentOrga -> SPARQLDeserializers.compareURIs(filter.getDirectChildURI(),parentOrga.getUri()))) {
                return false;
            }
            return Objects.isNull(restrictedOrganizationSet) || restrictedOrganizationSet.contains(org.getUri());
        }).collect(Collectors.toList());
    }

    public OrganizationModel create(OrganizationModel instance) throws Exception {
        sparql.create(instance);

        userOrganizationCache.invalidateAll();

        return instance;
    }

    /**
     * Gets an organization by its URI. Throws an exception if the current user does not have access to the organization.
     * See {@link #validateOrganizationAccess(URI, AccountModel)} for further information on access validation.
     *
     * @param uri The organization URI
     * @param user The current user
     * @return The organization
     * @throws Exception If the validation fails, or any other problem occurs
     */
    public OrganizationModel get(URI uri, AccountModel user) throws Exception {
        validateOrganizationAccess(uri, user);

        return sparql.getByURI(OrganizationModel.class, uri, user.getLanguage());
    }

    public List<OrganizationModel> getByURIs(List<URI> uris, String lang) throws Exception {
        return sparql.getListByURIs(
                OrganizationModel.class,
                uris,
                lang);
    }

    /**
     * Deletes an organization. Throws an exception if the current user does not have access to the organization.
     * See {@link #validateOrganizationAccess(URI, AccountModel)} for further information on access validation.
     *
     * @param uri The organization URI
     * @param user The current user
     * @throws Exception If the validation fails, or any other problem occurs
     */
    public void delete(URI uri, AccountModel user) throws Exception {
        validateOrganizationAccess(uri, user);

        sparql.delete(OrganizationModel.class, uri);

        userOrganizationCache.invalidateAll();
    }

    /**
     * Updates an organization. Throws an exception if the current user does not have access to the organization.
     * See {@link #validateOrganizationAccess(URI, AccountModel)} for further information on access validation. Also throws
     * an exception if the organization hierarchy is invalid (see {@link #validateOrganizationHierarchy(OrganizationModel)}).
     *
     * @param instance The organization to update
     * @param user The user
     * @return The updated organization
     * @throws Exception If the validation fails, or any other problem occurs
     */
    public OrganizationModel update(OrganizationModel instance, AccountModel user) throws Exception {
        validateOrganizationAccess(instance.getUri(), user);
        validateOrganizationHierarchy(instance);

        sparql.update(instance);

        userOrganizationCache.invalidateAll();

        return instance;
    }
}
