package org.opensilex.core.organisation.dal.site;

import com.mongodb.client.model.geojson.Geometry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.clauses.WhereClause;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.ORG;
import org.opensilex.core.external.geocoding.GeocodingService;
import org.opensilex.core.external.geocoding.OpenStreetMapGeocodingService;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.api.facility.FacilityAddressDTO;
import org.opensilex.core.organisation.api.site.SiteAddressDTO;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.OrganizationSPARQLHelper;
import org.opensilex.core.organisation.dal.OrganizationSearchFilter;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.exception.SiteFacilityInvalidAddressException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ForbiddenURIAccessException;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.server.exceptions.NotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.mapping.SPARQLListFetcher;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * @author Valentin RIGOLLE
 */
public class SiteDAO {

    protected final SPARQLService sparql;
    protected final MongoDBService nosql;
    protected final GeocodingService geocodingService;

    protected final GeospatialDAO geospatialDAO;
    protected final OrganizationDAO organizationDAO;
    protected final OrganizationSPARQLHelper organizationSPARQLHelper;

    protected final URI addressGraphURI;

    public SiteDAO(SPARQLService sparql, MongoDBService nosql) throws Exception {
        this(sparql, nosql, new OrganizationDAO(sparql, nosql));
    }

    public SiteDAO(SPARQLService sparql, MongoDBService nosql, OrganizationDAO organizationDao) throws Exception {
        this.sparql = sparql;
        this.nosql = nosql;

        this.geocodingService = new OpenStreetMapGeocodingService();
        this.geospatialDAO = new GeospatialDAO(nosql);
        this.organizationDAO = organizationDao;
        this.organizationSPARQLHelper = new OrganizationSPARQLHelper(sparql);

        this.addressGraphURI = sparql.getDefaultGraphURI(OrganizationModel.class);
    }

    /**
     * Search the sites among the ones accessible to the user. See {@link OrganizationSPARQLHelper#addSiteAccessClause(WhereClause, Var, Collection, URI)}
     * for further information on access validation. Organizations are retrieved using {@link SPARQLListFetcher}, so
     * only the URI field is accessible for the parent orgs.
     *
     * @param filter The search filters
     * @return The list of sites
     */
    public ListWithPagination<SiteModel> search(SiteSearchFilter filter)
            throws Exception {
        filter.validate();

        final List<URI> userOrganizations = filter.getSkipUserOrganizationFetch() // if user organizations have already been fetched, don't fetch it again
                ? filter.getUserOrganizations()
                : organizationDAO.search(new OrganizationSearchFilter()
                        .setRestrictedOrganizations(filter.getOrganizations())
                        .setUser(filter.getUser()))
                .stream().map(SPARQLResourceModel::getUri)
                .collect(Collectors.toList());

        AtomicReference<SelectBuilder> initialSelect = new AtomicReference<>();
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

            initialSelect.set(select);
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
     * Gets the geospatial model corresponding to the given site. There is no access check in this method, so please
     * make sure that the user has access to the given site by calling {@link #get(URI, AccountModel)} for example.
     *
     * @param siteUri The URI of the site
     * @return The geospatial model
     */
    public GeospatialModel getSiteGeospatialModel(URI siteUri) {
        return geospatialDAO.getGeometryByURI(siteUri, addressGraphURI);
    }

    /**
     * Gets a site by URI. Checks that the user has access to it beforehand. See {@link #validateSiteAccess(URI, AccountModel)}
     * for further information on  access validation.
     *
     * @param siteUri The URI of the site
     * @param user The current user
     * @return The site
     * @throws Exception If the access is not validated, or if any other problem occurs
     */
    public SiteModel get(URI siteUri, AccountModel user) throws Exception {
        validateSiteAccess(siteUri, user);

        return sparql.getByURI(SiteModel.class, siteUri, user.getLanguage());
    }

    /**
     * Gets a list of sites by URI. Checks that the user has access to them beforehand. Shorthand for calling {@link #search(SiteSearchFilter)}
     * with the {@link SiteSearchFilter#setSites(List)} filter.
     *
     * @param uris The URI of the sites
     * @param user The current user
     * @return The sites
     * @throws Exception If the access is not validated, or if any other problem occurs
     */
    public List<SiteModel> getList(List<URI> uris, AccountModel user) throws Exception {
        return search(new SiteSearchFilter()
                .setUser(user)
                .setSites(uris)).getList();
    }

    /**
     * Gets a site by a facility. Equivalent to calling {@link #search(SiteSearchFilter)} and using the {@link SiteSearchFilter#setFacility(URI)}
     * filter field.
     *
     * @param facilityUri The facility URI to search with
     * @param account     The current user
     * @return The corresponding sites
     */
    public List<SiteModel> getByFacility(URI facilityUri, AccountModel account) throws Exception {
        return search(new SiteSearchFilter()
                .setUser(account)
                .setFacility(facilityUri)
        ).getList();
    }

    /**
     * Creates a site. The address is checked before the creation (it must be the same as the associated facilities).
     * See {@link #validateSiteFacilityAddress(SiteModel, AccountModel)}.
     *
     * @param siteModel The site to create
     * @param user The current user
     * @return The created instance
     * @throws SiteFacilityInvalidAddressException If the address is invalid
     * @throws Exception If any other problem occurs
     */
    public SiteModel create(SiteModel siteModel, AccountModel user) throws Exception {
        validateSiteFacilityAddress(siteModel, user);

        if (!user.isAdmin()) {
            List<URI> siteOrganizationUriList = siteModel.getOrganizationURIListOrEmpty();
            if (siteOrganizationUriList.isEmpty()) {
                throw new BadRequestException("A site must be attached to at least one organization");
            }

            List<URI> userOrganizationList = organizationDAO.search(new OrganizationSearchFilter().setUser(user))
                    .stream().map(SPARQLResourceModel::getUri)
                    .collect(Collectors.toList());
            for (URI siteOrganizationUri : siteOrganizationUriList) {
                if (!userOrganizationList.contains(siteOrganizationUri)) {
                    throw new ForbiddenURIAccessException(siteOrganizationUri);
                }
            }
        }

        List<OrganizationModel> organizations = sparql.getListByURIs(
                OrganizationModel.class,
                siteModel.getOrganizationURIListOrEmpty(),
                user.getLanguage());
        siteModel.setOrganizations(organizations);
        sparql.create(siteModel);

        createSiteGeospatialModel(siteModel);

        return siteModel;
    }

    /**
     * Updates the site. Checks that the user has access to it beforehand. See {@link #validateSiteAccess(URI, AccountModel)}
     * for further information on  access validation. Also checks that the address is valid, see {@link #validateSiteFacilityAddress(SiteModel, AccountModel)}
     * for further information.
     *
     * @param siteModel    the site to update
     * @param accountModel The current user
     * @return The site
     * @throws Exception If the access is not validated, or if any other problem occurs
     */
    public SiteModel update(SiteModel siteModel, AccountModel accountModel) throws Exception {
        validateSiteAccess(siteModel.getUri(), accountModel);
        validateSiteFacilityAddress(siteModel, accountModel);

        List<OrganizationModel> organizations = sparql.getListByURIs(
                OrganizationModel.class,
                siteModel.getOrganizationURIListOrEmpty(),
                accountModel.getLanguage());
        siteModel.setOrganizations(organizations);

        SiteModel existingModel = sparql.getByURI(SiteModel.class, siteModel.getUri(), accountModel.getLanguage());

        if (existingModel == null) {
            throw new NotFoundException("Site URI not found : " + siteModel.getUri());
        }

        deleteSiteGeospatialModel(existingModel);
        createSiteGeospatialModel(siteModel);

        sparql.deleteByURI(sparql.getDefaultGraph(SiteModel.class), existingModel.getUri());
        sparql.create(siteModel);
        return siteModel;
    }

    /**
     * Deletes a site by URI. Checks that the user has access to it beforehand. See {@link #validateSiteAccess(URI, AccountModel)}
     * for further information on  access validation.
     *
     * @param uri     The URI of the site
     * @param account The current user
     * @throws Exception If the access is not validated, or if any other problem occurs
     */
    public void delete(URI uri, AccountModel account) throws Exception {
        validateSiteAccess(uri, account);

        SiteModel siteModel = sparql.getByURI(SiteModel.class, uri, account.getLanguage());

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

        this.geospatialDAO.create(new GeospatialModel(site, addressGraphURI, geom));
    }

    /**
     * Checks if a site address is valid (it must be the same as its facilities).
     *
     * @param siteModel The site
     * @param currentUser The current user
     * @throws SiteFacilityInvalidAddressException If the address is invalid
     */
    protected void validateSiteFacilityAddress(SiteModel siteModel, AccountModel currentUser) throws Exception {
        if (siteModel.getFacilities() == null || siteModel.getAddress() == null) {
            return;
        }

        List<URI> facilityUriList = siteModel.getFacilities()
                .stream().map(SPARQLResourceModel::getUri)
                .collect(Collectors.toList());

        List<FacilityModel> facilityModelList = sparql.getListByURIs(FacilityModel.class,
                facilityUriList, currentUser.getLanguage());

        for (FacilityModel facilityModel : facilityModelList) {
            if (facilityModel.getAddress() != null) {
                FacilityAddressDTO facilityAddress = new FacilityAddressDTO();
                facilityAddress.fromModel(facilityModel.getAddress());

                SiteAddressDTO siteAddress = new SiteAddressDTO();
                siteAddress.fromModel(siteModel.getAddress());

                if (!Objects.equals(facilityAddress, siteAddress)) {
                    throw new SiteFacilityInvalidAddressException(siteModel.getName(), facilityModel.getName());
                }
            }
        }
    }

    /**
     * Validates that the user has access to a site. Throws an exception if that is not the case. The
     * site must exist, and the user/site must satisfy at least one of the following conditions :
     *
     * <ul>
     *     <li>The user is admin</li>
     *     <li>The user has access to an organization containing the site</li>
     *     <li>The user is member of a group associated to the site</li>
     * </ul>
     *
     * @param siteURI The site URI to check
     * @param userModel The user
     * @throws IllegalArgumentException If the userModel is null
     */
    protected void validateSiteAccess(URI siteURI, AccountModel userModel) throws Exception {
        if (Objects.isNull(userModel)) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (!sparql.uriExists(SiteModel.class, siteURI)) {
            throw new NotFoundURIException(siteURI);
        }

        if (userModel.isAdmin()) {
            return;
        }

        List<URI> userOrganizations = organizationDAO.search(new OrganizationSearchFilter().setUser(userModel))
                .stream().map(SPARQLResourceModel::getUri)
                .collect(Collectors.toList());

        Var uriVar = makeVar(SiteModel.URI_FIELD);

        AskBuilder ask = new AskBuilder();
        ask.addWhere(uriVar, Ontology.typeSubClassAny, sparql.getRDFType(SiteModel.class));
        ask.addFilter(SPARQLQueryHelper.eq(uriVar, SPARQLDeserializers.nodeURI(siteURI)));
        organizationSPARQLHelper.addSiteAccessClause(ask, uriVar, userOrganizations, userModel.getUri());

        if (!sparql.executeAskQuery(ask)) {
            throw new ForbiddenURIAccessException(siteURI);
        }
    }
}
