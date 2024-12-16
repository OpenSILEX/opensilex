/*
 * *****************************************************************************
 *                         SiteDAO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 25/06/2024 10:10
 * Contact: gabriel.besombes@inrae.fr
 * *****************************************************************************
 */

package org.opensilex.core.organisation.dal.site;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.clauses.WhereClause;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.ORG;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.dal.OrganizationSPARQLHelper;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.mapping.SPARQLListFetcher;
import org.opensilex.sparql.mapping.SparqlNoProxyFetcher;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.util.*;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * @author Valentin RIGOLLE
 */
public class SiteDAO {

    protected final SPARQLService sparql;

    protected final OrganizationSPARQLHelper organizationSPARQLHelper;

    //#region constructor
    public SiteDAO(SPARQLService sparql) {
        this.sparql = sparql;

        this.organizationSPARQLHelper = new OrganizationSPARQLHelper(sparql);
    }
    //#endregion

    //#region public
    public void create(SiteModel siteModel) throws Exception {
        sparql.create(siteModel);
    }

    /**
     * Search the sites among the ones accessible to the user. See {@link OrganizationSPARQLHelper#addSiteAccessClause(WhereClause, Var, Collection, URI)}
     * for further information on access validation. Organizations are retrieved using {@link SPARQLListFetcher}, so
     * only the URI field is accessible for the parent orgs.
     *
     * @param filter The search filters
     * @param userOrganizations List of organization URI
     * @return The list of sites
     */
    public ListWithPagination<SiteModel> search(SiteSearchFilter filter, List<URI> userOrganizations) throws Exception {

        ListWithPagination<SiteModel> models = sparql.searchWithPagination(SiteModel.class, filter.getUser().getLanguage(), select -> {
            Var uriVar = makeVar(SiteModel.URI_FIELD);
            Var organizationsVar = makeVar("__" + SiteModel.ORGANIZATION_FIELD);

            organizationSPARQLHelper.addSiteAccessClause(select, uriVar, userOrganizations, filter.getUser().getUri());

            if (!StringUtils.isEmpty(filter.getNamePattern())) {
                select.addFilter(SPARQLQueryHelper.regexFilter(SiteModel.NAME_FIELD, filter.getNamePattern()));
            }

            if (!CollectionUtils.isEmpty(filter.getOrganizations())) {
                select.addWhere(organizationsVar, ORG.hasSite, uriVar);
                select.addFilter(SPARQLQueryHelper.inURIFilter(organizationsVar, filter.getOrganizations()));
            }

            if (CollectionUtils.isNotEmpty(filter.getSites())) {
                select.addFilter(SPARQLQueryHelper.inURIFilter(uriVar, filter.getSites()));
            }

            if (Objects.nonNull(filter.getFacility())) {
                select.addWhere(SPARQLDeserializers.nodeURI(filter.getFacility()), Oeso.withinSite, uriVar);
            }
        }, filter.getOrderByList(), filter.getPage(), filter.getPageSize());

        SPARQLListFetcher<SiteModel> listFetcher = new SPARQLListFetcher<>(
                sparql,
                SiteModel.class,
                sparql.getDefaultGraph(SiteModel.class),
                Collections.singleton(SiteModel.ORGANIZATION_FIELD),
                models.getList()
        );
        listFetcher.updateModels();

        return models;
    }

    /**
     * Use {@link SparqlNoProxyFetcher} to improve performance.
     *
     * @param currentUser The current user
     * @param userOrganizations List of organization URI
     * @return sites (not proxy) with their facility list associated
     * @throws Exception
     */
    public ListWithPagination<SiteModel> getSiteListWithFacilities(AccountModel currentUser, List<URI> userOrganizations) throws Exception {

        SparqlNoProxyFetcher<SiteModel> customFetcher = new SparqlNoProxyFetcher<>(SiteModel.class, sparql);

        ListWithPagination<SiteModel> models = sparql.searchWithPagination(
                sparql.getDefaultGraph(SiteModel.class),
                SiteModel.class,
                currentUser.getLanguage(),
                select -> organizationSPARQLHelper.addSiteAccessClause(select, makeVar(SiteModel.URI_FIELD), userOrganizations, currentUser.getUri()),
                Collections.emptyMap(),
                result -> customFetcher.getInstance(result,currentUser.getLanguage()),
                null,
                0,
                0);

        SPARQLListFetcher<SiteModel> listFetcher = new SPARQLListFetcher<>(
                sparql,
                SiteModel.class,
                sparql.getDefaultGraph(SiteModel.class),
                Collections.singleton(SiteModel.FACILITY_FIELD),
                models.getList()
        );
        listFetcher.updateModels();

        return models;
    }

    public SiteModel get(URI siteUri, AccountModel user) throws Exception {
        return sparql.getByURI(SiteModel.class, siteUri, user.getLanguage());
    }

    public SiteModel update(SiteModel siteModel) throws Exception {
        Node graph = sparql.getDefaultGraph(SiteModel.class);
        sparql.update(graph, siteModel);
        return siteModel;
    }

    public void delete(URI uri) throws Exception {
        sparql.delete(SiteModel.class, uri);
    }

    public boolean exists(URI siteUri) throws SPARQLException {
        return sparql.uriExists(SiteModel.class, siteUri);
    }

    /**
     * Validates that the user has access to a site. Throws an exception if that is not the case. The user/site must
     * satisfy at least one of the following conditions :
     *
     * <ul>
     *     <li>The user has access to an organization containing the site</li>
     *     <li>The user is member of a group associated to the site</li>
     * </ul>
     *
     * @apiNote Example of generated SPARQL query
     *
     * <pre>
     * ASK WHERE
     *   { ?uri rdf:type/(rdfs:subClassOf)* org:Site
     *     FILTER ( ?uri = <http://opensilex.test/id/organization/site_de_corroy> )
     *     OPTIONAL
     *       { ?_organization
     *                   org:hasSite  ?uri}
     *     OPTIONAL
     *       { ?uri      os-sec:hasGroup       ?groups .
     *         ?groups   os-sec:hasUserProfile  ?userProfiles .
     *         ?userProfiles
     *                   os-sec:hasUser        <http://opensilex.test/id/user/guest.opensilex>}
     *     FILTER ( ( ?_organization IN (<http://opensilex.test/id/organization/eurac_research>, <http://opensilex.test/id/organization/lepse>,...) ) || bound(?groups) )
     *   }
     * </pre>
     *
     * @param siteURI  The URI of the site
     * @param userOrganizations List of organization URI
     * @param currentUser current user
     * @return access authorization
     *
     */
    public boolean isUserAccessToSiteByOrganizationAndGroup(URI siteURI, List<URI> userOrganizations, AccountModel currentUser) throws Exception {

        Var uriVar = makeVar(SiteModel.URI_FIELD);

        AskBuilder ask = new AskBuilder();
        ask.addWhere(uriVar, Ontology.typeSubClassAny, sparql.getRDFType(SiteModel.class));
        ask.addFilter(SPARQLQueryHelper.eq(uriVar, SPARQLDeserializers.nodeURI(siteURI)));
        organizationSPARQLHelper.addSiteAccessClause(ask, uriVar, userOrganizations, currentUser.getUri());

        return sparql.executeAskQuery(ask);
    }
    //#endregion
}
