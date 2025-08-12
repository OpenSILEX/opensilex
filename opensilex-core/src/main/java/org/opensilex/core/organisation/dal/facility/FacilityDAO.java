/*
 * *****************************************************************************
 *                         FacilityDAO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 25/06/2024 10:10
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr, gabriel.besombes@inrae.fr
 * *****************************************************************************
 */
package org.opensilex.core.organisation.dal.facility;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.core.Var;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.bll.FacilityLogic;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.OrganizationSPARQLHelper;
import org.opensilex.core.organisation.dal.site.SiteModel;
import org.opensilex.core.variablesGroup.dal.VariablesGroupModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.mapping.SparqlNoProxyFetcher;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.schemaQuery.SparqlSchema;
import org.opensilex.sparql.service.schemaQuery.SparqlSchemaRootNode;
import org.opensilex.sparql.service.schemaQuery.SparqlSchemaSimpleNode;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * @author Valentin Rigolle
 */
public class FacilityDAO {

    private final SPARQLService sparql;
    private final OrganizationSPARQLHelper organizationSPARQLHelper;

    //#region constructor
    public FacilityDAO(SPARQLService sparql){
        this.sparql = sparql;
        this.organizationSPARQLHelper = new OrganizationSPARQLHelper(sparql);
    }

    //endregion

    //#region public
    public FacilityModel create(FacilityModel instance) throws Exception {
        sparql.create(instance);

        return instance;
    }

    public FacilityModel get(URI uri, String lang) throws Exception {
        return sparql.getByURI(FacilityModel.class, uri, lang);
    }

    public ListWithPagination<FacilityModel> search(FacilitySearchFilter filter, FacilityLogic.FacilitySearchRights organizationsAndSites) throws Exception {
        filter.validate();

        SparqlSchemaRootNode<FacilityModel> rootNode = new SparqlSchemaRootNode<>(
                sparql,
                FacilityModel.class,
                List.of(
                        new SparqlSchemaSimpleNode<>(OrganizationModel.class, FacilityModel.ORGANIZATION_FIELD),
                        new SparqlSchemaSimpleNode<>(SiteModel.class, FacilityModel.SITE_FIELD),
                        new SparqlSchemaSimpleNode<>(VariablesGroupModel.class, FacilityModel.VARIABLE_GROUPS_FIELD),
                        new SparqlSchemaSimpleNode<>(FacilityAddressModel.class, FacilityModel.ADDRESS_FIELD)
                ),
                true
        );

        SparqlSchema<FacilityModel> schema = new SparqlSchema<>(rootNode);

        return sparql.searchWithPaginationUsingSchema(
                FacilityModel.class,
                filter.getUser().getLanguage(),
                (select -> filterHandler(select, organizationsAndSites, filter)),
                schema,
                filter.getOrderByList(),
                filter.getPage(),
                filter.getPageSize()
        );
    }

    public ListWithPagination<FacilityModel> minimalSearch(FacilitySearchFilter filter, FacilityLogic.FacilitySearchRights organizationsAndSites) throws Exception {
        SparqlNoProxyFetcher<FacilityModel> customFetcher = new SparqlNoProxyFetcher<>(FacilityModel.class, sparql);

        return sparql.searchWithPagination(
                sparql.getDefaultGraph(FacilityModel.class),
                FacilityModel.class,
                filter.getUser().getLanguage(),
                (select -> filterHandler(select, organizationsAndSites, filter)),
                Collections.emptyMap(),
                result -> customFetcher.getInstance(result, filter.getLang()),
                filter.getOrderByList(),
                filter.getPage(),
                filter.getPageSize()
        );
    }

    public void delete(URI uri) throws Exception {
        sparql.delete(FacilityModel.class, uri);
    }

    public FacilityModel update(FacilityModel instance) throws Exception {
        sparql.update(instance);

        return instance;
    }

    public boolean exists(URI facilityUri) throws SPARQLException {
        return sparql.uriExists(FacilityModel.class, facilityUri);
    }

    /**
     * Validates that the user has access to a facility. Throws an exception if that is not the case. The user/facility must
     * satisfy at least one of the following conditions :
     *
     * <ul>
     *     <li>The user has access to an organization hosted by the facility</li>
     *     <li>The user has access to a site containing the facility</li>
     * </ul>
     *
     * @apiNote Example of generated SPARQL query
     *
     * <pre>
     * ASK WHERE
     * { <http://opensilex.test/id/organization/facility....>
     *       rdf:type  ?rdfType .
     *      ?rdfType (rdfs:subClassOf)* vocabulary:Facility}
     *
     * </pre>
     *
     * @param facilityURI  The URI of the site
     * @param userSites List of site URI
     * @param userOrganizations List of organization URI
     * @param accountModel current user
     * @return access authorization
     *
     */
    public boolean isUserAccessToFacilityByOrganizationAndSite(URI facilityURI, List<URI> userSites, List<URI> userOrganizations,AccountModel accountModel) throws Exception {
        Var uriVar = makeVar(FacilityModel.URI_FIELD);

        AskBuilder ask = new AskBuilder();
        ask.addWhere(uriVar, Ontology.typeSubClassAny, sparql.getRDFType(FacilityModel.class));
        ask.addFilter(SPARQLQueryHelper.eq(uriVar, SPARQLDeserializers.nodeURI(facilityURI)));
        organizationSPARQLHelper.addFacilityAccessClause(ask, uriVar, userOrganizations, userSites, accountModel.getUri());

        return sparql.executeAskQuery(ask);
    }
    //endregion

    //#region private
    private void filterHandler(SelectBuilder select, FacilityLogic.FacilitySearchRights organizationsAndSites, FacilitySearchFilter filter) throws Exception {
        Var uriVar = makeVar(FacilityModel.URI_FIELD);

        if (!filter.getUser().isAdmin()) {
            organizationSPARQLHelper.addFacilityAccessClause(select,uriVar, organizationsAndSites.getUserOrganizations(), organizationsAndSites.getUserSites(), filter.getUser().getUri());
        }

        // Facilities filter
        if (CollectionUtils.isNotEmpty(filter.getFacilities())) {
            select.addFilter(SPARQLQueryHelper.inURIFilter(uriVar, filter.getFacilities()));
        }

        // Organization filter
        if (CollectionUtils.isNotEmpty(filter.getOrganizations())) {
            select.addWhere(makeVar(FacilityModel.ORGANIZATION_FIELD), Oeso.isHosted, uriVar);
            select.addFilter(SPARQLQueryHelper.inURIFilter(makeVar(FacilityModel.ORGANIZATION_FIELD), filter.getOrganizations()));
        }

        if (!StringUtils.isEmpty(filter.getPattern())) {
            select.addFilter(SPARQLQueryHelper.regexFilter(SiteModel.NAME_FIELD, filter.getPattern()));
        }
    }
    //endregion
}
