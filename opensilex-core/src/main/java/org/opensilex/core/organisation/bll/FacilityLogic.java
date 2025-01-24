/*
 * *****************************************************************************
 *                         FacilityLogic.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 26/09/2024 15:06
 * Contact: alexia.chiavarino@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 *
 *
 * *****************************************************************************
 *
 */

package org.opensilex.core.organisation.bll;

import com.mongodb.client.ClientSession;
import com.mongodb.client.model.geojson.Geometry;
import org.opensilex.core.external.geocoding.GeocodingService;
import org.opensilex.core.external.geocoding.OpenStreetMapGeocodingService;
import org.opensilex.core.location.bll.LocationLogic;
import org.opensilex.core.location.bll.LocationObservationCollectionLogic;
import org.opensilex.core.location.bll.LocationObservationLogic;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.api.facility.FacilityAddressDTO;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.OrganizationSearchFilter;
import org.opensilex.core.organisation.dal.facility.FacilityDAO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.facility.FacilitySearchFilter;
import org.opensilex.core.organisation.dal.site.SiteModel;
import org.opensilex.core.organisation.dal.site.SiteSearchFilter;
import org.opensilex.core.organisation.exception.SiteFacilityInvalidAddressException;
import org.opensilex.nosql.distributed.SparqlMongoTransaction;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ForbiddenURIAccessException;
import org.opensilex.server.exceptions.InvalidValueException;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class FacilityLogic {

    private SPARQLService sparql;
    private MongoDBServiceV2 mongodb;
    private FacilityDAO facilityDAO;
    private OrganizationDAO organizationDAO;
    private SiteLogic siteLogic;
    private final GeocodingService geocodingService;


    //#region constructor
    public FacilityLogic(SPARQLService sparql, MongoDBServiceV2 mongodb) throws Exception {
        this.sparql = sparql;
        this.mongodb = mongodb;
        this.organizationDAO = new OrganizationDAO(sparql);
        this.facilityDAO = new FacilityDAO(sparql);
        this.siteLogic = new SiteLogic(sparql, mongodb);
        this.geocodingService = new OpenStreetMapGeocodingService();
    }
    //#endregion

    //region public

    /**
     * Creates a facility. The address is checked before the creation (it must be the same as the associated sites).
     * See {@link #validateFacilityAddress(FacilityModel, AccountModel)}.
     *
     * @param instance  The facility to create
     * @param locations geometry list linked to the facility
     * @param user      The current user
     * @return The created instance
     * @throws SiteFacilityInvalidAddressException If the address is invalid
     * @throws Exception                           If any other problem occurs
     */
    public FacilityModel create(FacilityModel instance, List<LocationObservationModel> locations, AccountModel user) throws Exception {
        validateFacilityAddress(instance, user);
        validateFacilityRelations(instance, user);

        String lang = null;
        if (Objects.nonNull(user)) {
            lang = user.getLanguage();
            instance.setPublisher(user.getUri());
        }

        List<OrganizationModel> organizationModels = organizationDAO.getByURIs(instance.getOrganizationUris(), lang);
        instance.setOrganizations(organizationModels);

        new SparqlMongoTransaction(sparql, mongodb).execute(session -> {
            facilityDAO.create(instance);
            if (Objects.nonNull(locations) || Objects.nonNull(instance.getAddress())) {
                createFacilityLocations(session, instance, locations);
            }
            return null;
        });

        organizationDAO.invalidateCache();
        return instance;
    }

    /**
     * Gets a facility by URI. Checks that the user has access to it beforehand. See {@link #validateFacilityAccess(URI, AccountModel)}
     * for further information on  access validation.
     *
     * @param uri  The URI of the facility
     * @param user The current user
     * @return The facility
     */
    public FacilityModel get(URI uri, AccountModel user) throws ForbiddenURIAccessException {
        try {
            validateFacilityAccess(uri, user);
            return facilityDAO.get(uri, user.getLanguage());
        } catch (ForbiddenURIAccessException exception) {
            throw new ForbiddenURIAccessException(uri, "You don't have the rights to access this facility : " + uri.toString());
        } catch (Exception e) {
            throw new NotFoundURIException(uri);
        }
    }

    /**
     * Gets a list of facilities by URI. Shorthand for calling {@link #search(FacilitySearchFilter)} with the
     * {@link FacilitySearchFilter#setFacilities(List)} filter.
     *
     * @param uris The URI of the facilities
     * @param user The current user
     * @return The facilities
     * @throws Exception If the access is not validated, or if any other problem occurs
     */
    public List<FacilityModel> getList(List<URI> uris, AccountModel user) throws Exception {
        return search(new FacilitySearchFilter()
                .setUser(user)
                .setFacilities(uris)).getList();
    }

    /**
     * Search the facilities among the ones accessible to the user.
     *
     * @param filter The search filters
     * @return The list of facilities
     */
    public ListWithPagination<FacilityModel> search(FacilitySearchFilter filter) throws Exception {
        try {
            filter.validate();

            FacilitySearchRights organizationsAndSites = calculateUserSearchRights(filter);

            return facilityDAO.search(filter, organizationsAndSites);
        } catch (Exception e) {
            throw new InvalidValueException(e.getMessage());
        }
    }

    /**
     * Search the facilities among the ones accessible to the user.
     * Minimal amount of fields loaded. Embedded object lists are not loaded, embedded single objects only have name and type loaded.
     *
     * @param filter The search filters
     * @return The list of facilities
     */
    public ListWithPagination<FacilityModel> minimalSearch(FacilitySearchFilter filter) throws Exception {
        filter.validate();

        FacilitySearchRights organizationsAndSites = calculateUserSearchRights(filter);

        return facilityDAO.minimalSearch(filter, organizationsAndSites);
    }

    /**
     * Search facilities with detail (sparql) and, if filter is at 'true', only facilities with location (mongo).
     *
     * @param endDate the date before which the location existed
     * @param currentUser The current user
     * @return a Map of facilities with or without corresponding location
     * @throws Exception If some error is encountered during the search
     */
    public Map<FacilityModel, LocationObservationModel> getFacilitiesWithPosition(Instant endDate, AccountModel currentUser) throws Exception {

        FacilitySearchFilter facilitySearchfilter = new FacilitySearchFilter();

        //Set searchFilter
        facilitySearchfilter.setUser(currentUser);
        facilitySearchfilter.setPageSize(0);
        facilitySearchfilter.setPage(0);

        FacilitySearchRights organizationsAndSites = calculateUserSearchRights(facilitySearchfilter);

        List<FacilityModel> facilityList = facilityDAO.minimalSearch(facilitySearchfilter, organizationsAndSites).getList();

        LocationObservationLogic locationObservationLogic = new LocationObservationLogic(mongodb);
        return locationObservationLogic.generateModelObservationCollectionMap(
                facilityList,
                FacilityModel::getLocationObservationCollection,
                Objects.nonNull(endDate) ? endDate : Instant.now()
        );
    }

    /**
     * Updates the facility. Checks that the user has access to it beforehand. See {@link #validateFacilityAccess(URI, AccountModel)}
     * for further information on  access validation. Also checks that the address is valid, see {@link #validateFacilityAddress(FacilityModel, AccountModel)}
     * for further information.
     *
     * @param instance the facility to update
     * @param locations location list to update
     * @param user  The current user
     * @return The facility
     * @throws Exception If the access is not validated, or if any other problem occurs
     */
    public FacilityModel update(FacilityModel instance, List<LocationObservationModel> locations, AccountModel user) throws Exception {
        validateFacilityAccess(instance.getUri(), user);
        validateFacilityAddress(instance, user);
        validateFacilityRelations(instance, user);

        List<OrganizationModel> organizationModels = organizationDAO.getByURIs(instance.getOrganizationUris(), user.getLanguage());
        instance.setOrganizations(organizationModels);

        FacilityModel existingModel = facilityDAO.get(instance.getUri(), user.getLanguage());

        new SparqlMongoTransaction(sparql, mongodb).execute(session -> {
            facilityDAO.update(instance);

            if(Objects.nonNull(locations) || Objects.nonNull(instance.getAddress())){
                if(Objects.nonNull(existingModel.getLocationObservationCollection())){
                    updateFacilityLocations(session, instance, existingModel, locations);
                }else{
                    createFacilityLocations(session, instance, locations);
                }
            }else{
                deleteFacilityLocations(session, instance);
            }

            return null;
        });

        organizationDAO.invalidateCache();
        return instance;
    }

    /**
     * Deletes a facility by URI. Checks that the user has access to it beforehand. See {@link #validateFacilityAccess(URI, AccountModel)}
     * for further information on  access validation.
     *
     * @param uri  The URI of the facility
     * @param user The current user
     * @throws Exception If the access is not validated, or if any other problem occurs
     */
    public void delete(URI uri, AccountModel user) throws Exception {
        validateFacilityAccess(uri, user);

        FacilityModel model = facilityDAO.get(uri, user.getLanguage());

        new SparqlMongoTransaction(sparql, mongodb).execute(session -> {
            deleteFacilityLocations(session, model);
            facilityDAO.delete(uri);

            return null;
        });
        organizationDAO.invalidateCache();
    }

    /**
     * Gets the last Location Observation model corresponding to the given facility. There is no access check in this method, so please
     * make sure that the user has access to the given facility (by calling {@link #get(URI, AccountModel)}, for example.
     *
     * @param facilityModel The facility
     * @return The Location Observation model
     */
    public LocationObservationModel getLastFacilityLocationModel(FacilityModel facilityModel) {
        LocationObservationLogic locationObservationLogic = new LocationObservationLogic(mongodb);

        List<LocationObservationModel> lastLocationByFacility = locationObservationLogic.getLastLocationObservation(
                Collections.singletonList(facilityModel.getLocationObservationCollection()),
                false,
                Instant.now()
        );

        if (lastLocationByFacility.isEmpty()) {
            return null;
        } else {
            return lastLocationByFacility.get(0);
        }
    }
    //#endregion

    //region Search rights
    private FacilitySearchRights calculateUserSearchRights(FacilitySearchFilter filter) throws Exception {
        List<URI> userOrganizations = filter.getUser().isAdmin() ? null :
                organizationDAO.search(new OrganizationSearchFilter()
                                .setRestrictedOrganizations(filter.getOrganizations())
                                .setUser(filter.getUser()))
                        .stream().map(SPARQLResourceModel::getUri)
                        .collect(Collectors.toList());

        List<URI> userSites = filter.getUser().isAdmin() ? null :
                siteLogic.search(new SiteSearchFilter()
                                .setUser(filter.getUser())
                                .setOrganizations(filter.getOrganizations())
                                .setUserOrganizations(userOrganizations)
                                .setSkipUserOrganizationFetch(true))
                        .getList().stream().map(SPARQLResourceModel::getUri)
                        .collect(Collectors.toList());

        return new FacilitySearchRights(userOrganizations, userSites);
    }

    /**
     * Mini class to contain results of search rights calculation.
     */
    public static class FacilitySearchRights {
        final List<URI> userOrganizations;
        final List<URI> userSites;

        public FacilitySearchRights(List<URI> userOrganizations, List<URI> userSites) {
            this.userOrganizations = userOrganizations;
            this.userSites = userSites;
        }

        public List<URI> getUserOrganizations() {
            return userOrganizations;
        }

        public List<URI> getUserSites() {
            return userSites;
        }
    }
    //endregion

    //#region private

    /**
     * Validates that the user has access to a facility. Throws an exception if that is not the case. The
     * facility must exist, and the user/facility must satisfy at least one of the following conditions :
     *
     * <ul>
     *     <li>The user is admin</li>
     *     <li>The user has access to an organization hosted by the facility</li>
     *     <li>The user has access to a site containing the facility</li>
     *     <li>The user is the publisher of the facility</li>
     * </ul>
     *
     * @param facilityURI  The facility URI to check
     * @param accountModel The user
     * @throws IllegalArgumentException If the AccountModel is null
     */
    private void validateFacilityAccess(URI facilityURI, AccountModel accountModel) throws Exception {
        if (Objects.isNull(accountModel)) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (!facilityDAO.exists(facilityURI)) {
            throw new NotFoundURIException(facilityURI);
        }

        if (accountModel.isAdmin()) {
            return;
        }

        final List<URI> userOrganizations = organizationDAO.search(new OrganizationSearchFilter().setUser(accountModel))
                .stream().map(SPARQLResourceModel::getUri)
                .collect(Collectors.toList());

        final List<URI> userSites = siteLogic.search(new SiteSearchFilter()
                        .setUser(accountModel)
                        .setUserOrganizations(userOrganizations)
                        .setSkipUserOrganizationFetch(true))
                .getList().stream().map(SPARQLResourceModel::getUri)
                .collect(Collectors.toList());

        if (!facilityDAO.isUserAccessToFacilityByOrganizationAndSite(facilityURI, userSites, userOrganizations, accountModel)) {
            throw new ForbiddenURIAccessException(facilityURI);
        }
    }

    /**
     * Checks if a facility address is valid (it must be the same as its sites).
     *
     * @param facilityModel The facility
     * @throws SiteFacilityInvalidAddressException If the address is invalid
     */
    private void validateFacilityAddress(FacilityModel facilityModel, AccountModel user) {

        if (Objects.isNull(facilityModel.getAddress())) {
            return;
        }

        if (Objects.isNull(facilityModel.getSites())) {
            return;
        }

        List<SiteModel> siteModelList = facilityModel.getSites().stream().map(site -> {
            try {
                return this.siteLogic.get(site.getUri(), user);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        for (SiteModel siteModel : siteModelList) {
            if (Objects.nonNull(siteModel.getAddress())) {
                SiteLogic.assertEqualsFacilityAndSiteAddresses(siteModel, facilityModel);
            }
        }
    }

    private void validateFacilityRelations(FacilityModel facilityModel, AccountModel user) throws SPARQLException, URISyntaxException {
        if (!facilityModel.getRelations().isEmpty()) {
            OntologyDAO ontoDAO = new OntologyDAO(sparql);
            ClassModel model = ontoDAO.getClassModel(facilityModel.getType(), new URI(Oeso.Facility.getURI()), user.getLanguage());
            URI graph = sparql.getDefaultGraphURI(FacilityModel.class);

            for (SPARQLModelRelation relation : facilityModel.getRelations()) {
                if (!ontoDAO.validateThenAddObjectRelationValue(graph, model, URI.create(relation.getProperty().getURI()), relation.getValue(), facilityModel)) {
                    throw new InvalidValueException("Invalid relation value for " + relation.getProperty().toString() + " => " + relation.getValue());
                }
            }
        }
    }

    private void createFacilityLocations(ClientSession session, FacilityModel facility, List<LocationObservationModel> locationObservationModels) throws Exception {
        LocationObservationLogic locationObservationLogic = new LocationObservationLogic(mongodb);
        List<LocationObservationModel> locations = new ArrayList<>();

        if (Objects.isNull(locationObservationModels) && Objects.nonNull(facility.getAddress())) {
            Geometry geometry = convertAddressToGeometry(facility);
            if (Objects.isNull(geometry)) {
                return;
            }
            LocationObservationModel locationObservationModel = new LocationObservationModel();
            locationObservationModel.setLocation(LocationLogic.buildLocationModel(geometry, null, null, null, null));

            locations.add(locationObservationModel);
        } else {
            locationObservationModels.forEach(location -> locationObservationLogic.validateDates(location.getEndDate(), location.getStartDate()));
            locations = locationObservationModels;
        }
        //Create the LocationObservationCollection
        LocationObservationCollectionLogic locationObservationCollectionLogic = new LocationObservationCollectionLogic(sparql);
        URI locationObservationCollectionUri = locationObservationCollectionLogic.createLocationObservationCollection(facility.getUri());
        //Create LocationObservations
        locationObservationLogic.createLocationObservations(session, locationObservationCollectionUri, facility.getUri(), locations, true);
    }

    private void updateFacilityLocations(ClientSession session, FacilityModel instance, FacilityModel existingModel, List<LocationObservationModel> locationObservationModels) throws Exception {
        //Delete existing
        LocationObservationLogic locationObservationLogic = new LocationObservationLogic(mongodb);
        locationObservationLogic.deleteLocationObservations(session, existingModel.getLocationObservationCollection().getUri());

        //Create new locations
        List<LocationObservationModel> locations = new ArrayList<>();

        if ((Objects.isNull(locationObservationModels) || locationObservationModels.isEmpty()) && !Objects.isNull(instance.getAddress())) {
            Geometry geometry = convertAddressToGeometry(instance);
            if (Objects.isNull(geometry)) {
                return;
            }

            LocationObservationModel locationObservationModel = new LocationObservationModel();
            locationObservationModel.setLocation(LocationLogic.buildLocationModel(geometry, null, null, null, null));

            locations.add(locationObservationModel);
        } else {
            locationObservationModels.forEach(location -> locationObservationLogic.validateDates(location.getEndDate(), location.getStartDate()));
            locations = locationObservationModels;
        }

        locationObservationLogic.createLocationObservations(session, existingModel.getLocationObservationCollection().getUri(), existingModel.getUri(), locations, true);
    }

    private Geometry convertAddressToGeometry(FacilityModel facility) {
        FacilityAddressDTO addressDto = new FacilityAddressDTO();
        addressDto.fromModel(facility.getAddress());

        return geocodingService.getPointFromAddress(addressDto.toReadableAddress());
    }

    private void deleteFacilityLocations(ClientSession session, FacilityModel facility) {
        if (facility.getLocationObservationCollection() != null) {
            LocationObservationLogic locationObservationLogic = new LocationObservationLogic(mongodb);
            LocationObservationCollectionLogic locationObservationCollectionLogic = new LocationObservationCollectionLogic(sparql);

            try {
                locationObservationLogic.deleteLocationObservations(session, facility.getLocationObservationCollection().getUri());
                locationObservationCollectionLogic.deleteLocationObservationCollection(facility.getLocationObservationCollection().getUri());
            } catch (Exception e) {
                throw new NotFoundURIException("Invalid or unknown URI ", facility.getLocationObservationCollection().getUri());
            }
        }
    }
    //#endregion
}

