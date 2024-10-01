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
import org.geojson.GeoJsonObject;
import org.opensilex.core.external.geocoding.GeocodingService;
import org.opensilex.core.external.geocoding.OpenStreetMapGeocodingService;
import org.opensilex.core.location.bll.LocationLogic;
import org.opensilex.core.location.bll.LocationObservationCollectionLogic;
import org.opensilex.core.location.bll.LocationObservationLogic;
import org.opensilex.core.location.dal.LocationModel;
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
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ForbiddenURIAccessException;
import org.opensilex.server.exceptions.InvalidValueException;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.ws.rs.NotAllowedException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
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
        this.facilityDAO = new FacilityDAO(sparql,mongodb,organizationDAO);
        this.siteLogic = new SiteLogic(sparql,mongodb);
        this.geocodingService = new OpenStreetMapGeocodingService();
    }
    //#endregion

    //region public
    /**
     * Creates a facility. The address is checked before the creation (it must be the same as the associated sites).
     * See {@link #validateFacilityAddress(FacilityModel, AccountModel)}.
     *
     * @param instance The facility to create
     * @param geometry geometry linked to the facility
     * @param user The current user
     * @return The created instance
     * @throws SiteFacilityInvalidAddressException If the address is invalid
     * @throws Exception If any other problem occurs
     */
    public FacilityModel create(FacilityModel instance, GeoJsonObject geometry, Instant date, Instant endDate, AccountModel user) throws Exception {
        validateFacilityAddress(instance, user);
        validateFacilityRelations(instance, user);

        String lang = null;
        if (user != null) {
            lang = user.getLanguage();
            instance.setPublisher(user.getUri());
        }

        List<OrganizationModel> organizationModels = organizationDAO.getByURIs(instance.getOrganizationUris(),lang);
        instance.setOrganizations(organizationModels);

        new SparqlMongoTransaction(sparql, mongodb).execute(session -> {
            facilityDAO.create(instance);
            if(geometry != null || instance.getAddress() != null) {
                createFacilityLocationModel(session, instance, geometry, date, endDate);
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
     * @param uri The URI of the facility
     * @param user The current user
     * @return The facility
     * @throws Exception If the access is not validated, or if any other problem occurs
     */
    public FacilityModel get(URI uri, AccountModel user) throws Exception {
        validateFacilityAccess(uri, user);
        return facilityDAO.get(uri, user.getLanguage());
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
        filter.validate();

        FacilitySearchRights organizationsAndSites = calculateUserSearchRights(filter);

        return facilityDAO.search(filter,organizationsAndSites);
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

        return facilityDAO.minimalSearch(filter,organizationsAndSites);
    }

    /**
     * Search facilities with detail (sparql) and, if filter is at 'true', only facilities with location (mongo).
     *
     * @param uris Facility list
     * @param filterWithLocation Filter facilities with location
     * @param currentUser The current user
     * @throws Exception If some error is encountered during the search
     *
     * @return a Map of facilities with or without corresponding location
     */
    public Map<FacilityModel, LocationObservationModel> getDetailsWithGeometry(List<URI> uris, boolean filterWithLocation, AccountModel currentUser) throws Exception {
        FacilityDAO facilityDAO = new FacilityDAO(sparql,mongodb,organizationDAO);
        Map<FacilityModel, LocationObservationModel> facilitiesAndLocationsMap = new HashMap<>();
        FacilitySearchFilter facilitySearchfilter = new FacilitySearchFilter();

       /* //Set searchFilter
        facilitySearchfilter.setUser(currentUser);

        if (!uris.isEmpty()) {
            facilitySearchfilter.setFacilities(uris);
        }

        FacilitySearchRights organizationsAndSites = calculateUserSearchRights(facilitySearchfilter);

        List<FacilityModel> facilityList = facilityDAO.search(facilitySearchfilter, organizationsAndSites ).getList();

        //Get only facility with location
        if (filterWithLocation) {
            Map<FacilityModel, URI> facilitiesWithLocationMap = facilityList.stream()
                    .filter(facility -> facility.getLocationObservationCollection() != null)
                    .collect(Collectors.toMap(Function.identity(), FacilityModel::getLocationObservationCollection, (oldValue, newValue) -> oldValue));

            if (!facilitiesWithLocationMap.isEmpty()) {
                LocationObservationLogic locationObservationLogic = new LocationObservationLogic(mongodb);
                //the 'hasGeometry' parameter must be set to 'true' because this is the only type of location stored in mongo that is allowed for the facility
                List<LocationObservationModel> locationObservationModels = locationObservationLogic.getLastLocationObservation(new ArrayList<>(facilitiesWithLocationMap.values()), true, null, null);

                facilitiesWithLocationMap.forEach((k, v) -> {
                    LocationObservationModel value = locationObservationModels.stream()
                            .filter(location -> SPARQLDeserializers.compareURIs(v, location.getUri()))
                            .findAny().orElseThrow(NullPointerException::new);
                    facilitiesAndLocationsMap.put(k, value);
                });
            }
        } else {
            facilityList.forEach(facility -> facilitiesAndLocationsMap.put(facility,null));
        }*/
        return facilitiesAndLocationsMap;
    }

    /**
     * Updates the facility. Checks that the user has access to it beforehand. See {@link #validateFacilityAccess(URI, AccountModel)}
     * for further information on  access validation. Also checks that the address is valid, see {@link #validateFacilityAddress(FacilityModel, AccountModel)}
     * for further information.
     *
     * @param instance the facility to update
     * @param user The current user
     * @return The facility
     * @throws Exception If the access is not validated, or if any other problem occurs
     */
    public FacilityModel update(FacilityModel instance, GeoJsonObject geometry,Instant date, Instant endDate, AccountModel user) throws Exception {
        validateFacilityAccess(instance.getUri(), user);
        validateFacilityAddress(instance, user);
        validateFacilityRelations(instance, user);

        List<OrganizationModel> organizationModels = organizationDAO.getByURIs(instance.getOrganizationUris(),user.getLanguage());
        instance.setOrganizations(organizationModels);

        FacilityModel existingModel = facilityDAO.get(instance.getUri(), user.getLanguage());

        new SparqlMongoTransaction(sparql, mongodb).execute(session -> {
            if (Objects.nonNull(existingModel.getAddress()) || Objects.nonNull(geometry)) {
                deleteFacilityLocationModel(session, instance);

                //TODO: utile?
                /* if (Objects.nonNull(existingModel.getAddress())) {
                    sparql.delete(FacilityAddressModel.class, existingModel.getAddress().getUri());
                }*/
            }

            createFacilityLocationModel(session, instance, geometry,date,endDate);
            facilityDAO.update(instance);

            return null;
        });

        organizationDAO.invalidateCache();
        return instance;
    }

    /**
     * Deletes a facility by URI. Checks that the user has access to it beforehand. See {@link #validateFacilityAccess(URI, AccountModel)}
     * for further information on  access validation.
     *
     * @param uri The URI of the facility
     * @param user The current user
     * @throws Exception If the access is not validated, or if any other problem occurs
     */
    public void delete(URI uri, AccountModel user) throws Exception {
        validateFacilityAccess(uri, user);

        FacilityModel model = facilityDAO.get(uri, user.getLanguage());

        new SparqlMongoTransaction(sparql,mongodb).execute(session ->{
            deleteFacilityLocationModel(session, model);

            //TODO : utile?
            /*if (Objects.nonNull(model.getAddress())) {
                sparql.delete(FacilityAddressModel.class, model.getAddress().getUri());
            }*/

            facilityDAO.delete(uri);
            return null;
        });
        organizationDAO.invalidateCache();
    }

    public FacilitySearchFilter createSearchFilter(String pattern, List<URI> organizations, int page, int pageSize, List<OrderBy> orderByList, AccountModel currentUser) throws Exception {
        FacilitySearchFilter filter = (FacilitySearchFilter) new FacilitySearchFilter()
                .setUser(currentUser)
                .setPattern(pattern)
                .setOrderByList(orderByList)
                .setPage(page)
                .setPageSize(pageSize);
        if (!organizations.isEmpty()) {
            filter.setOrganizations(organizations);
        }
        return filter;
    }

    /**
     * Gets the last Location Observation model corresponding to the given facility. There is no access check in this method, so please
     * make sure that the user has access to the given facility (by calling {@link #get(URI, AccountModel)}, for example.
     *
     * @param facilityModel The facility
     * @return The Location Observation model
     */
    public LocationObservationModel getFacilityLocationModel(FacilityModel facilityModel) {
        if (facilityModel.getLocationObservationCollection() != null) {
            LocationObservationLogic locationObservationLogic = new LocationObservationLogic(mongodb);

            return locationObservationLogic.getLastLocationObservation(
                    Collections.singletonList(facilityModel.getLocationObservationCollection()),
                    true,
                    Instant.now(),
                    null).get(0);
        } else {
            return new LocationObservationModel();
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
    public static class FacilitySearchRights{
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
     * @param facilityURI The facility URI to check
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

        if (facilityModel.getAddress() == null) {
            return;
        }

        if (facilityModel.getSites() == null) {
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
            if (siteModel.getAddress() != null) {
                SiteLogic.assertEqualsFacilityAndSiteAddresses(siteModel, facilityModel);
            }
        }
    }

    private void validateFacilityRelations(FacilityModel facilityModel, AccountModel user) throws SPARQLException, URISyntaxException {
        if (facilityModel.getRelations() != null) {
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

    /**
     * Checks if a facility with geometry (not from an address) is valid :
     *  - it must have one observation date;
     *  - if there is a endDate, it must be after the "begin" date.
     *
     * @param date observation date of the geometry
     * @param endDate end observation date of the geometry
     * @throws NotAllowedException If dates are invalid
     */
    private void validateDates(Instant date, Instant endDate){
        if(Objects.isNull(date)){
            throw new NotAllowedException("Date cannot be null");
        }
        if(!Objects.isNull(endDate) && endDate.isBefore(date)){
            throw new NotAllowedException("Date cannot be before endDate");
        }
    }

    private void createFacilityLocationModel(ClientSession session, FacilityModel facility, GeoJsonObject geojson, Instant date, Instant endDate) throws Exception {
        Geometry geometry;

        /*if (Objects.isNull(geometry) && Objects.isNull(facility.getAddress())) {
            //TODO: pourquoi delete si pas encore créé?
            //deleteFacilityGeospatialModel(facility.getUri());
            return;
        }*/

        if (Objects.isNull(geojson) && !Objects.isNull(facility.getAddress())) {
            FacilityAddressDTO addressDto = new FacilityAddressDTO();
            addressDto.fromModel(facility.getAddress());

            geometry = geocodingService.getPointFromAddress(addressDto.toReadableAddress());
            if (Objects.isNull(geometry)) {
                return;
            }
        } else {
            validateDates(date, endDate);
            geometry = LocationLogic.geoJsonToGeometry(geojson);
        }
        //Create the LocationObservationCollection
        LocationObservationCollectionLogic locationObservationCollectionLogic = new LocationObservationCollectionLogic(sparql);
        URI locationObservationCollectionUri = locationObservationCollectionLogic.createLocationObservationCollection(facility.getUri());
        //Create the LocationObservation
        LocationObservationLogic locationObservationLogic = new LocationObservationLogic(mongodb);
        LocationModel locationModel = LocationLogic.buildLocationModel(geometry, null, null, null, null);

        locationObservationLogic.createLocationObservation(session, locationObservationCollectionUri, true, date, endDate, locationModel);
    }

    private void deleteFacilityLocationModel(ClientSession session, FacilityModel facility) {
        if(facility.getLocationObservationCollection()  != null) {
            LocationObservationLogic locationObservationLogic = new LocationObservationLogic(mongodb);
            LocationObservationCollectionLogic locationObservationCollectionLogic = new LocationObservationCollectionLogic(sparql);

            try {
                LocationObservationModel locationObservationModel = locationObservationLogic.getLocationObservationByURI(facility.getLocationObservationCollection().getUri());
                if (locationObservationModel != null) {
                    locationObservationLogic.delete(session, facility.getLocationObservationCollection().getUri());
                    locationObservationCollectionLogic.deleteLocationObservationCollection(facility.getLocationObservationCollection().getUri());
                }
            } catch (NoSQLInvalidURIException e) {
                throw new NotFoundURIException("Invalid or unknown data URI ", facility.getLocationObservationCollection().getUri());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    //#endregion
}

