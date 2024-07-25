package org.opensilex.core.organisation.bll;

import com.mongodb.client.model.geojson.Geometry;
import org.apache.commons.collections4.CollectionUtils;
import org.opensilex.core.external.geocoding.GeocodingService;
import org.opensilex.core.external.geocoding.OpenStreetMapGeocodingService;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.core.organisation.api.facility.FacilityAddressDTO;
import org.opensilex.core.organisation.api.site.SiteAddressDTO;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.OrganizationSearchFilter;
import org.opensilex.core.organisation.dal.facility.FacilityDAO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.site.SiteAddressModel;
import org.opensilex.core.organisation.dal.site.SiteDAO;
import org.opensilex.core.organisation.dal.site.SiteModel;
import org.opensilex.core.organisation.dal.site.SiteSearchFilter;
import org.opensilex.core.organisation.exception.SiteFacilityInvalidAddressException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ForbiddenURIAccessException;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.server.exceptions.NotFoundException;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SiteLogic {

    private final SPARQLService sparql;
    private final MongoDBService nosql;

    private final SiteDAO siteDAO;
    private final OrganizationDAO organizationDAO;
    private final GeospatialDAO geospatialDAO;

    private final URI addressGraphURI;
    private final GeocodingService geocodingService;

    //#region constructor

    public SiteLogic(SPARQLService sparql, MongoDBService mongodb) throws Exception {
        this.sparql = sparql;
        this.nosql = mongodb;

        this.siteDAO = new SiteDAO(sparql);
        this.organizationDAO = new OrganizationDAO(sparql);
        this.geospatialDAO = new GeospatialDAO(mongodb);

        this.addressGraphURI = sparql.getDefaultGraphURI(OrganizationModel.class);
        this.geocodingService = new OpenStreetMapGeocodingService();
    }
    //#endregion

    //#region public
    /**
     * Creates a site. The address is checked before the creation (it must be the same as the associated facilities).
     * See {@link #validateSiteFacilityAddress(SiteModel, AccountModel)}.
     *
     * @param siteModel The site to create
     * @return The created instance
     * @throws SiteFacilityInvalidAddressException If the address is invalid
     * @throws Exception If any other problem occurs
     */
    public SiteModel create(SiteModel siteModel, AccountModel currentUser) throws Exception {
        if (CollectionUtils.isEmpty(siteModel.getOrganizations())) {
            throw new BadRequestException("A site must be attached to at least one organization");
        }

        validateSiteFacilityAddress(siteModel, currentUser);

        siteModel.setPublisher(currentUser.getUri());

        if (!currentUser.isAdmin()) {
            List<URI> siteOrganizationUriList = siteModel.getOrganizationURIListOrEmpty();

            if (siteOrganizationUriList.isEmpty()) {
                throw new BadRequestException("A site must be attached to at least one organization");
            }

            List<URI> userOrganizationList = getListOrganizationUris(new OrganizationSearchFilter().setUser(currentUser));

            for (URI siteOrganizationUri : siteOrganizationUriList) {
                if (!userOrganizationList.contains(siteOrganizationUri)) {
                    throw new ForbiddenURIAccessException(siteOrganizationUri);
                }
            }
        }

        List<OrganizationModel> organizations = organizationDAO.getByURIs(siteModel.getOrganizationURIListOrEmpty(),currentUser.getLanguage());

        siteModel.setOrganizations(organizations);

        siteDAO.create(siteModel);

        createSiteGeospatialModel(siteModel);
        organizationDAO.invalidateCache();

        return siteModel;
    }
    /**
     * Gets a site by URI. Checks that the user has access to it beforehand. See {@link #validateSiteAccess(URI,AccountModel)}
     * for further information on  access validation.
     *
     * @param siteUri The URI of the site
     * @param currentUser current user
     * @return The site
     * @throws Exception If the access is not validated, or if any other problem occurs
     */
    public SiteModel get(URI siteUri, AccountModel currentUser) throws Exception {
        validateSiteAccess(siteUri, currentUser);

        return siteDAO.get(siteUri, currentUser);
    }

    /**
     * Gets a list of sites by URI. Checks that the user has access to them beforehand. Shorthand for calling {@link #search(SiteSearchFilter)}
     * with the {@link SiteSearchFilter#setSites(List)} filter.
     *
     * @param uris The URI of the sites
     * @return The sites
     * @throws Exception If the access is not validated, or if any other problem occurs
     */
    public List<SiteModel> getList(List<URI> uris, AccountModel currentUser) throws Exception {
        return search(new SiteSearchFilter()
                .setUser(currentUser)
                .setSites(uris)).getList();
    }

    /**
     * Search the sites among the ones accessible to the user.
     *
     * @param filter The search filters
     * @return The list of sites
     */
    public ListWithPagination<SiteModel> search(SiteSearchFilter filter) throws Exception {
        filter.validate();

        final List<URI> userOrganizations = filter.getSkipUserOrganizationFetch() // if user organizations have already been fetched, don't fetch it again
                ? filter.getUserOrganizations()
                : getListOrganizationUris(new OrganizationSearchFilter()
                        .setRestrictedOrganizations(filter.getOrganizations())
                        .setUser(filter.getUser()));

        return siteDAO.search(filter, userOrganizations);
    }

    /**
     * Updates the site. Checks that the user has access to it beforehand. See {@link #validateSiteAccess(URI,AccountModel)}
     * for further information on  access validation. Also checks that the address is valid, see {@link #validateSiteFacilityAddress(SiteModel, AccountModel)}
     * for further information.
     *
     * @param siteModel the site to update
     * @param currentUser current user
     * @return The site
     * @throws Exception If the access is not validated, or if any other problem occurs
     */
    public SiteModel update(SiteModel siteModel, AccountModel currentUser) throws Exception {
        if (CollectionUtils.isEmpty(siteModel.getOrganizations())) {
            throw new BadRequestException("A site must be attached to at least one organization");
        }

        validateSiteAccess(siteModel.getUri(), currentUser);
        validateSiteFacilityAddress(siteModel, currentUser);

        List<OrganizationModel> organizations = organizationDAO.getByURIs(siteModel.getOrganizationURIListOrEmpty(),currentUser.getLanguage());
        siteModel.setOrganizations(organizations);

        SiteModel existingModel = siteDAO.get(siteModel.getUri(), currentUser);

        if (existingModel == null) {
            throw new NotFoundException("Site URI not found : " + siteModel.getUri());
        }

        updateSiteGeospatialModel(siteModel);
        siteDAO.update(siteModel);
        organizationDAO.invalidateCache();

        return siteModel;
    }

    /**
     * Deletes a site by URI. Checks that the user has access to it beforehand. See {@link #validateSiteAccess(URI, AccountModel)}
     * for further information on  access validation.
     *
     * @param uri The URI of the site
     * @param currentUser current user
     * @throws Exception If the access is not validated, or if any other problem occurs
     */
    public void delete(URI uri, AccountModel currentUser) throws Exception {
        SiteModel siteModel = siteDAO.get(uri, currentUser);
        deleteSiteGeospatialModel(siteModel);

        siteDAO.delete(uri);
        organizationDAO.invalidateCache();
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
     * Gets a site by a facility. Equivalent to calling {@link #search(SiteSearchFilter)} and using the {@link SiteSearchFilter#setFacility(URI)}
     * filter field.
     *
     * @param facilityUri The facility URI to search with
     * @param account  The current user
     * @return The corresponding sites
     */
    public List<SiteModel> getByFacility(URI facilityUri, AccountModel account) throws Exception {
        return search(new SiteSearchFilter()
                .setUser(account)
                .setFacility(facilityUri)
        ).getList();
    }

    public static void assertEqualsFacilityAndSiteAddresses(SiteModel siteModel, FacilityModel facilityModel) {
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

    //#endregion

    //#region private
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
     * @throws IllegalArgumentException If the userModel is null
     */
    private void validateSiteAccess(URI siteURI, AccountModel currentUser) throws Exception {
        if (Objects.isNull(currentUser)) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (!siteDAO.exists(siteURI)) {
            throw new NotFoundURIException(siteURI);
        }

        if (Boolean.TRUE.equals(currentUser.isAdmin())) {
            return;
        }

        List<URI> userOrganizations = getListOrganizationUris(new OrganizationSearchFilter().setUser(currentUser));

        if (!siteDAO.isUserAccessToSiteByOrganizationAndGroup(siteURI, userOrganizations, currentUser)) {
            throw new ForbiddenURIAccessException(siteURI);
        }
    }

    /**
     * Checks if a site address is valid (it must be the same as its facilities).
     *
     * @param siteModel The site
     * @param currentUser The current user
     * @throws SiteFacilityInvalidAddressException If the address is invalid
     */
    protected void validateSiteFacilityAddress(SiteModel siteModel, AccountModel currentUser) throws Exception {
        if (siteModel.getFacilities() == null || siteModel.getFacilities().isEmpty() || siteModel.getAddress() == null) {
            return;
        }

        List<URI> facilityUriList = siteModel.getFacilities()
                .stream().map(SPARQLResourceModel::getUri)
                .collect(Collectors.toList());

        List<FacilityModel> facilityModelList = new FacilityDAO(sparql,nosql,organizationDAO).getList(facilityUriList,currentUser);

        for (FacilityModel facilityModel : facilityModelList) {
            if (facilityModel.getAddress() != null) {
                assertEqualsFacilityAndSiteAddresses(siteModel, facilityModel);
            }
        }
    }

    protected List<URI> getListOrganizationUris(OrganizationSearchFilter filter) throws Exception {

       return organizationDAO.search(filter)
                .stream().map(SPARQLResourceModel::getUri)
                .collect(Collectors.toList());
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

    protected void updateSiteGeospatialModel(SiteModel site) throws Exception {
        if (site.getAddress() == null) {
            return;
        }

        SiteAddressDTO addressDto = new SiteAddressDTO();
        addressDto.fromModel(site.getAddress());

        Geometry geom = geocodingService.getPointFromAddress(addressDto.toReadableAddress());

        if (geom == null) {
            return;
        }

        this.geospatialDAO.update(new GeospatialModel(site, addressGraphURI, geom),site.getUri(),sparql.getDefaultGraphURI(SiteModel.class));
    }

    private void deleteSiteGeospatialModel(SiteModel siteModel) throws Exception {
        if (siteModel.getAddress() == null) {
            return;
        }

        if (this.geospatialDAO.getGeometryByURI(siteModel.getUri(), addressGraphURI) != null) {
            this.geospatialDAO.delete(siteModel.getUri(), addressGraphURI);
        }

        sparql.delete(SiteAddressModel.class, siteModel.getAddress().getUri());
    }
    //#endregion

}
