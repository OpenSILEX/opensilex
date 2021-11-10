package org.opensilex.core.organisation.api.site;

import com.mongodb.client.model.geojson.Geometry;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.arq.querybuilder.clauses.WhereClause;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.ORG;
import org.opensilex.core.external.geocoding.GeocodingService;
import org.opensilex.core.external.geocoding.OpenStreetMapGeocodingService;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.dal.*;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ForbiddenURIAccessException;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.exceptions.NotFoundException;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

public class SiteDAO {

    protected final SPARQLService sparql;
    protected final MongoDBService nosql;
    protected final GeocodingService geocodingService;

    protected final GeospatialDAO geospatialDAO;
    protected final InfrastructureDAO organizationDAO;
    protected final OrganizationSPARQLHelper organizationSPARQLHelper;

    protected final URI addressGraphURI;

    public SiteDAO(SPARQLService sparql, MongoDBService nosql) throws Exception {
        this.sparql = sparql;
        this.nosql = nosql;

        this.geocodingService = new OpenStreetMapGeocodingService();
        this.geospatialDAO = new GeospatialDAO(nosql);
        this.organizationDAO = new InfrastructureDAO(sparql, nosql);
        this.organizationSPARQLHelper = new OrganizationSPARQLHelper(sparql);

        this.addressGraphURI = sparql.getDefaultGraphURI(InfrastructureModel.class);
    }

    public ListWithPagination<SiteModel> searchSites(UserModel user, String pattern, List<URI> organizations,
                                                     List<OrderBy> orderByList, Integer page, Integer pageSize)
            throws Exception {
        Set<URI> userOrganizations = organizationDAO.getUserInfrastructures(user);

        final Set<URI> userSites = getUserSites(user);

        // Filter by organizations
        if (organizations != null && !organizations.isEmpty()) {
            if (userOrganizations != null) {
                userOrganizations.retainAll(organizations);
            } else {
                userOrganizations = new HashSet<>(organizations);
            }
        }

        if (userOrganizations != null && userOrganizations.isEmpty()) {
            return new ListWithPagination<>(Collections.emptyList());
        }

        final Set<URI> finalOrganizations = userOrganizations;

        return sparql.searchWithPagination(
                SiteModel.class,
                user.getLanguage(),
                (select -> {
                    if (finalOrganizations != null) {
                        select.addWhere(makeVar(SiteModel.ORGANIZATION_FIELD), Oeso.isHosted, makeVar(SiteModel.URI_FIELD));
                        SPARQLQueryHelper.inURI(select, SiteModel.ORGANIZATION_FIELD, finalOrganizations);
                    }

                    if (userSites != null) {
                        SPARQLQueryHelper.inURI(select, SiteModel.URI_FIELD, userSites);
                    }

                    // REGEX
                    if (!StringUtils.isEmpty(pattern)) {
                        select.addFilter(SPARQLQueryHelper.regexFilter(SiteModel.NAME_FIELD, pattern));
                    }
                }),
                orderByList,
                page,
                pageSize
        );
    }

    public SiteModel getSite(URI siteUri, UserModel user) throws Exception {
        checkUserAccess(siteUri, user);

        return sparql.getByURI(SiteModel.class, siteUri, user.getLanguage());
    }

    public SiteModel createSite(SiteModel siteModel, UserModel user) throws Exception {
        //@todo check l'accès aux orgs ?

        List<InfrastructureModel> organizations = sparql.getListByURIs(
                InfrastructureModel.class,
                siteModel.getOrganizationURIListOrEmpty(),
                user.getLanguage());
        siteModel.setOrganizations(organizations);
        sparql.create(siteModel);

        createSiteGeospatialModel(siteModel);

        return siteModel;
    }

    public SiteModel updateSite(SiteModel siteModel, UserModel user) throws Exception {
        checkUserAccess(siteModel.getUri(), user);

        List<InfrastructureModel> organizations = sparql.getListByURIs(
                InfrastructureModel.class,
                siteModel.getOrganizationURIListOrEmpty(),
                user.getLanguage());
        siteModel.setOrganizations(organizations);

        SiteModel existingModel = sparql.getByURI(SiteModel.class, siteModel.getUri(), user.getLanguage());

        if (existingModel == null) {
            throw new NotFoundException("Site URI not found : " + siteModel.getUri());
        }

        deleteSiteGeospatialModel(existingModel);
        createSiteGeospatialModel(siteModel);

        sparql.deleteByURI(sparql.getDefaultGraph(SiteModel.class), existingModel.getUri());
        sparql.create(siteModel);
        return siteModel;
    }

    public void deleteSite(URI uri, UserModel user) throws Exception {
        checkUserAccess(uri, user);

        SiteModel siteModel = sparql.getByURI(SiteModel.class, uri, user.getLanguage());

        deleteSiteGeospatialModel(siteModel);

        sparql.delete(SiteModel.class, uri);
    }

    protected void deleteSiteGeospatialModel(SiteModel siteModel) throws Exception {
        if (siteModel.getAddress() == null) {
            return;
        }

        if (this.geospatialDAO.getGeometryByURI(siteModel.getUri(), addressGraphURI) != null) {
            this.geospatialDAO.delete(siteModel.getUri(), addressGraphURI);
        }

        sparql.delete(SiteAddressModel.class, siteModel.getAddress().getUri());
    }

    protected void createSiteGeospatialModel(SiteModel site) {
        if (site.getAddress() == null) {
            return;
        }

        SiteAddressDTO addressDto = new SiteAddressDTO();
        addressDto.fromModel(site.getAddress());

        Geometry geom = geocodingService.getPointFromAddress(addressDto.toReadableAddress());

        if (geom == null) {
            return;
        }

        GeospatialModel geospatialModel = new GeospatialModel();
        geospatialModel.setUri(site.getUri());
        geospatialModel.setName(site.getName());
        geospatialModel.setRdfType(site.getType());
        geospatialModel.setGraph(addressGraphURI);
        geospatialModel.setGeometry(geom);

        this.geospatialDAO.create(geospatialModel);
    }

    /**
     * Retourne les URI des sites auxquels l'utilisateur a accès.
     *
     * @param userModel
     * @return
     */
    public Set<URI> getUserSites(UserModel userModel) throws Exception {
        if (userModel.isAdmin()) {
            return null;
        }

        List<URI> siteUris = sparql.searchURIs(SiteModel.class, userModel.getLanguage(), select -> {
            Var siteUriVar = makeVar(SiteModel.URI_FIELD);

            addSiteAccessClause(select, siteUriVar, userModel);
        });

        return new HashSet<>(siteUris);
    }

    protected void checkUserAccess(URI siteUri, UserModel userModel) throws Exception {
        if (!sparql.uriExists(SiteModel.class, siteUri)) {
            throw new NotFoundException("Site URI not found : " + siteUri);
        }

        if (userModel == null || userModel.isAdmin()) {
            return;
        }

        AskBuilder ask = sparql.getUriExistsQuery(SiteModel.class, siteUri);
        addSiteAccessClause(ask, siteUri, userModel);

        if (!sparql.executeAskQuery(ask)) {
            throw new ForbiddenURIAccessException(siteUri);
        }
    }

    protected void addSiteAccessClause(WhereClause<?> where, Object siteUri, UserModel userModel) throws Exception {
        Var organizationVar = makeVar("_organization");
        Var groupVar = makeVar(SiteModel.GROUP_FIELD);
        Var profileVar = makeVar(GroupModel.USER_PROFILES_FIELD);

        WhereBuilder userInOrgGroup = new WhereBuilder();
        userInOrgGroup.addWhere(organizationVar, ORG.hasSite, siteUri);
        userInOrgGroup.addWhere(organizationSPARQLHelper.buildOrganizationGroupUserClause(organizationVar, userModel.getUri()));

        WhereBuilder userInSiteGroup = new WhereBuilder();
        userInSiteGroup.addWhere(siteUri, SecurityOntology.hasGroup, groupVar);
        userInSiteGroup.addWhere(groupVar, SecurityOntology.hasUserProfile, profileVar);
        userInSiteGroup.addWhere(profileVar, SecurityOntology.hasUser, userModel.getUri());

        WhereBuilder userInOrgOrInSiteGroup = new WhereBuilder();
        userInOrgOrInSiteGroup.addWhere(userInOrgGroup);
        userInOrgOrInSiteGroup.addUnion(userInSiteGroup);

        where.addWhere(userInOrgOrInSiteGroup);
    }
}
