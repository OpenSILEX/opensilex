/*
 * *****************************************************************************
 *                         SiteLogic.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 26/07/2024 14:09
 * Contact: alexia.chiavarino@inrae.fr
 * *****************************************************************************
 *
 */

package org.opensilex.core.organisation.bll;

import com.mongodb.client.ClientSession;
import com.mongodb.client.model.geojson.Geometry;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.vocabulary.ORG;
import org.opensilex.core.external.geocoding.GeocodingService;
import org.opensilex.core.external.geocoding.OpenStreetMapGeocodingService;
import org.opensilex.core.location.bll.LocationLogic;
import org.opensilex.core.location.bll.LocationObservationCollectionLogic;
import org.opensilex.core.location.bll.LocationObservationLogic;
import org.opensilex.core.location.dal.LocationModel;
import org.opensilex.core.location.dal.LocationObservationCollectionModel;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.core.organisation.api.facility.FacilityAddressDTO;
import org.opensilex.core.organisation.api.site.SiteAddressDTO;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.OrganizationSearchFilter;
import org.opensilex.core.organisation.dal.facility.FacilityDAO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.site.SiteDAO;
import org.opensilex.core.organisation.dal.site.SiteModel;
import org.opensilex.core.organisation.dal.site.SiteSearchFilter;
import org.opensilex.core.organisation.exception.SiteFacilityInvalidAddressException;
import org.opensilex.nosql.distributed.SparqlMongoTransaction;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ForbiddenURIAccessException;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.server.exceptions.NotFoundException;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import javax.naming.SizeLimitExceededException;
import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SiteLogic {

    private final SPARQLService sparql;
    private final MongoDBService nosql;

    private final SiteDAO siteDAO;
    private final OrganizationDAO organizationDAO;

    private final GeocodingService geocodingService;

    //#region constructor

    public SiteLogic(SPARQLService sparql, MongoDBService mongodb) throws Exception {
        this.sparql = sparql;
        this.nosql = mongodb;

        this.siteDAO = new SiteDAO(sparql);
        this.organizationDAO = new OrganizationDAO(sparql);
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

        //Validate rdfType
        if(siteModel.getType() != null){
            try{
                SPARQLModule.getOntologyStoreInstance().getClassModel(
                        siteModel.getType(),
                        URI.create(ORG.Site.getURI()),
                        currentUser.getLanguage()
                );
            } catch (SPARQLInvalidURIException e){
                throw new NotFoundURIException("rdfType URI not found :", siteModel.getType());
            }
        }

        if (CollectionUtils.isEmpty(siteModel.getOrganizations())) {
            throw new BadRequestException("A site must be attached to at least one organization");
        }

        validateSiteFacilityAddress(siteModel, currentUser);

        if (Boolean.FALSE.equals(currentUser.isAdmin())) {
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
        siteModel.setPublisher(currentUser.getUri());

        new SparqlMongoTransaction(sparql,nosql.getServiceV2()).execute(session ->{
            siteDAO.create(siteModel);

            if (siteModel.getAddress() != null) {
                createSiteLocation(session,siteModel);
            }
            return null;
        });

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

        new SparqlMongoTransaction(sparql, nosql.getServiceV2()).execute(session -> {

            siteDAO.update(siteModel);

            if (existingModel.getAddress() != null && siteModel.getAddress() != null) {
                updateSiteLocation(session, siteModel);
            } else if (existingModel.getAddress() == null && siteModel.getAddress() != null) {
                createSiteLocation(session, siteModel);
            } else if (existingModel.getAddress() != null && siteModel.getAddress() == null) {
                deleteSiteLocation(session, siteModel);
            }

            return null;
        });

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

        new SparqlMongoTransaction(sparql,nosql.getServiceV2()).execute(session ->{
            siteDAO.delete(uri);
            deleteSiteLocation(session, siteModel);
            return null;
        });
        organizationDAO.invalidateCache();
    }

    /**
     * Gets the observation model corresponding to the given site. There is no access check in this method, so please
     * make sure that the user has access to the given site by calling {@link #get(URI, AccountModel)} for example.
     *
     * @param siteModel the site to get
     * @return The location observation model
     */
    public LocationObservationModel getSiteLocationObservationModel(SiteModel siteModel) {
        if(siteModel.getLocationObservationCollection() != null) {
            LocationObservationLogic locationObservationLogic = new LocationObservationLogic(nosql.getServiceV2());
            try {
            return locationObservationLogic.getLocationObservationByURI(siteModel.getLocationObservationCollection().getUri());
            } catch (NoSQLInvalidURIException e) {
                throw new NotFoundURIException("Invalid or unknown data URI ", siteModel.getLocationObservationCollection().getUri());
            }
        } else {
            return new LocationObservationModel();
        }
    }

    /**
     * Search sites only with location and with facility list.
     *
     * @param currentUser The current user
     * @throws Exception If some error is encountered during the search
     *
     * @return a Map of sites with or without corresponding location
     */
    public Map<SiteModel, LocationObservationModel> getSitesWithPosition(AccountModel currentUser) throws Exception {
        Map<SiteModel, LocationObservationModel> sitesAndLocationsMap = new HashMap<>();


        List<URI> userOrganizations = organizationDAO.search(new OrganizationSearchFilter()
                        .setUser(currentUser))
                .stream().map(SPARQLResourceModel::getUri)
                .collect(Collectors.toList());

        List<SiteModel> siteList = siteDAO.getSiteListWithFacilities(currentUser, userOrganizations).getList();

        //Get site list with location
        Map<SiteModel, LocationObservationCollectionModel> sitesWithLocationMap = siteList.stream()
                .filter(site -> site.getLocationObservationCollection() != null)
                .collect(Collectors.toMap(Function.identity(), SiteModel::getLocationObservationCollection));

        if (!sitesWithLocationMap.isEmpty()) {
            LocationObservationLogic locationObservationLogic = new LocationObservationLogic(nosql.getServiceV2());
            // filter only on the List URI because there is only one "geometry" type location and no date required
            //the 'hasGeometry' parameter must be set to 'true' because this is the only type of location stored in mongo that is allowed for the site
            List<LocationObservationModel> locationObservationModels = locationObservationLogic.getLastLocationObservation(new ArrayList<>(sitesWithLocationMap.values()), true, null, null);

            if (locationObservationModels.isEmpty()) {
                throw new NotFoundException("No location found");
            } else if (locationObservationModels.size() < sitesWithLocationMap.size()) {
                throw new NegativeArraySizeException("Missing location(s)");
            } else if (locationObservationModels.size() > sitesWithLocationMap.size()) {
                throw new SizeLimitExceededException("authorized location number for site is exceed");
            }

            var locationObservationMap = locationObservationModels.stream()
                    .collect(Collectors.toMap(LocationObservationModel::getObservationCollection, Function.identity()));

            sitesWithLocationMap.forEach((site, collection) -> {
                var observation = locationObservationMap.get(collection.getUri());
                sitesAndLocationsMap.put(site, observation);
            });
        }
        return sitesAndLocationsMap;
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
    private void validateSiteFacilityAddress(SiteModel siteModel, AccountModel currentUser) throws Exception {
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

    private List<URI> getListOrganizationUris(OrganizationSearchFilter filter) throws Exception {

       return organizationDAO.search(filter)
                .stream().map(SPARQLResourceModel::getUri)
                .collect(Collectors.toList());
    }

    private void createSiteLocation(ClientSession session, SiteModel siteModel) throws Exception {
        Geometry geom = convertAddressToGeometry(siteModel);

        if (geom != null) {
            //Create the LocationObservationCollection
            LocationObservationCollectionLogic locationObservationCollectionLogic = new LocationObservationCollectionLogic(sparql);
            URI locationObservationCollectionUri = locationObservationCollectionLogic.createLocationObservationCollection(siteModel.getUri());
            //Create the LocationObservation
            LocationObservationLogic locationObservationLogic = new LocationObservationLogic(nosql.getServiceV2());

            checkUniqueObservation(locationObservationCollectionUri);

            LocationModel locationModel = LocationLogic.buildLocationModel(geom, null, null, null, null);
            locationObservationLogic.createLocationObservation(session, locationObservationCollectionUri, siteModel.getUri(), true, locationModel);
        }
    }

    private void updateSiteLocation(ClientSession session, SiteModel siteModel) {
        Geometry geom = convertAddressToGeometry(siteModel);

        if (geom != null) {
            //Update the LocationObservation
            LocationObservationLogic locationObservationLogic = new LocationObservationLogic(nosql.getServiceV2());
            LocationModel locationModel = LocationLogic.buildLocationModel(geom, null, null, null, null);
            locationObservationLogic.updateLocationObservation(session, siteModel.getLocationObservationCollection().getUri(), true, locationModel);
        }
    }

    private Geometry convertAddressToGeometry(SiteModel siteModel) {
        SiteAddressDTO addressDto = new SiteAddressDTO();
        addressDto.fromModel(siteModel.getAddress());

        return geocodingService.getPointFromAddress(addressDto.toReadableAddress());
    }

    private void deleteSiteLocation(ClientSession session, SiteModel siteModel) throws Exception {
        if (siteModel.getAddress() == null && siteModel.getLocationObservationCollection() == null) {
            return;
        }

        if (siteModel.getLocationObservationCollection() != null) {
            LocationObservationLogic locationObservationLogic = new LocationObservationLogic(nosql.getServiceV2());
            LocationObservationCollectionLogic locationObservationCollectionLogic = new LocationObservationCollectionLogic(sparql);

            try {
                LocationObservationModel locationObservationModel = locationObservationLogic.getLocationObservationByURI(siteModel.getLocationObservationCollection().getUri());
                if (locationObservationModel != null) {
                    locationObservationLogic.delete(session, siteModel.getLocationObservationCollection().getUri());
                    locationObservationCollectionLogic.deleteLocationObservationCollection(siteModel.getLocationObservationCollection().getUri());
                }
            } catch (NoSQLInvalidURIException e) {
                throw new NotFoundURIException("Invalid or unknown data URI ", siteModel.getLocationObservationCollection().getUri());
            }

        }
    }

    private void checkUniqueObservation(URI observationCollectionUri) throws Exception {
        LocationObservationLogic locationObservationLogic = new LocationObservationLogic(nosql.getServiceV2());

        try {
            LocationObservationModel model = locationObservationLogic.getLocationObservationByURI(observationCollectionUri);

            if (model != null) {
                throw new SizeLimitExceededException("a site can have only one location observation");
            }
        } catch (NoSQLInvalidURIException e) {
            // Ignore exception
        }
    }
    //#endregion

}
