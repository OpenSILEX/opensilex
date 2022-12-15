/*******************************************************************************
 *                         FacilityDAO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2022.
 * Last Modification: 28/10/2022
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.organisation.dal.facility;

import com.mongodb.client.model.geojson.Geometry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.clauses.WhereClause;
import org.apache.jena.sparql.core.Var;
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
import org.opensilex.core.organisation.dal.site.SiteDAO;
import org.opensilex.core.organisation.dal.site.SiteModel;
import org.opensilex.core.organisation.dal.site.SiteSearchFilter;
import org.opensilex.core.organisation.exception.SiteFacilityInvalidAddressException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.ForbiddenURIAccessException;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * @author Valentin Rigolle
 */
public class FacilityDAO {

    private final SPARQLService sparql;
    private final GeocodingService geocodingService;
    private final GeospatialDAO geospatialDAO;

    private final OrganizationDAO organizationDAO;
    private final SiteDAO siteDAO;
    private final URI addressGraphURI;

    private final OrganizationSPARQLHelper organizationSPARQLHelper;

    public FacilityDAO(SPARQLService sparql, MongoDBService nosql, OrganizationDAO organizationDAO) throws Exception {
        this.sparql = sparql;

        this.geocodingService = new OpenStreetMapGeocodingService();
        this.geospatialDAO = new GeospatialDAO(nosql);
        this.organizationDAO = organizationDAO;
        this.siteDAO = new SiteDAO(sparql, nosql, organizationDAO);
        this.addressGraphURI = sparql.getDefaultGraphURI(OrganizationModel.class);

        this.organizationSPARQLHelper = new OrganizationSPARQLHelper(sparql);
    }

    /**
     * Creates a facility. The address is checked before the creation (it must be the same as the associated sites).
     * See {@link #validateFacilityAddress(FacilityModel, UserModel)}.
     *
     * @param instance The facility to create
     * @param user The current user
     * @return The created instance
     * @throws SiteFacilityInvalidAddressException If the address is invalid
     * @throws Exception If any other problem occurs
     */
    public FacilityModel create(FacilityModel instance, UserModel user) throws Exception {
        validateFacilityAddress(instance, user);

        String lang = null;
        if (user != null) {
            lang = user.getLanguage();
        }
        List<OrganizationModel> organizationModels = sparql.getListByURIs(OrganizationModel.class, instance.getInfrastructureUris(), lang);
        instance.setInfrastructures(organizationModels);
        sparql.create(instance);

        createFacilityGeospatialModel(instance);

        return instance;
    }

    /**
     * Gets a facility by URI. Checks that the user has access to it beforehand. See {@link #validateFacilityAccess(URI, UserModel)}
     * for further information on  access validation.
     *
     * @param uri The URI of the facility
     * @param user The current user
     * @return The facility
     * @throws Exception If the access is not validated, or if any other problem occurs
     */
    public FacilityModel get(URI uri, UserModel user) throws Exception {
        validateFacilityAccess(uri, user);
        return sparql.getByURI(FacilityModel.class, uri, user.getLanguage());
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
    public List<FacilityModel> getList(List<URI> uris, UserModel user) throws Exception {
        return search(new FacilitySearchFilter()
                .setUser(user)
                .setFacilities(uris)).getList();
    }

    /**
     * Search the facilities among the ones accessible to the user. See {@link OrganizationSPARQLHelper#addFacilityAccessClause(WhereClause, Var, Collection, Collection, URI)}
     * for further information on access validation.
     *
     * @param filter The search filters
     * @return The list of facilities
     */
    public ListWithPagination<FacilityModel> search(FacilitySearchFilter filter) throws Exception {
        filter.validate();

        final List<URI> userOrganizations = filter.getUser().isAdmin() ? null :
                organizationDAO.search(null, filter.getOrganizations(), filter.getUser())
                .stream().map(SPARQLResourceModel::getUri)
                .collect(Collectors.toList());

        final List<URI> userSites = filter.getUser().isAdmin() ? null :
                siteDAO.search(new SiteSearchFilter()
                                .setUser(filter.getUser())
                                .setOrganizations(filter.getOrganizations())
                                .setUserOrganizations(userOrganizations)
                                .setSkipUserOrganizationFetch(true))
                .getList().stream().map(SPARQLResourceModel::getUri)
                .collect(Collectors.toList());

        return sparql.searchWithPagination(
                FacilityModel.class,
                filter.getUser().getLanguage(),
                (select -> {
                    Var uriVar = makeVar(FacilityModel.URI_FIELD);

                    if (!filter.getUser().isAdmin()) {
                        organizationSPARQLHelper.addFacilityAccessClause(select, uriVar, userOrganizations, userSites, filter.getUser().getUri());
                    }

                    // Facilities filter
                    if (CollectionUtils.isNotEmpty(filter.getFacilities())) {
                        select.addFilter(SPARQLQueryHelper.inURIFilter(uriVar, filter.getFacilities()));
                    }

                    // Organization filter
                    if (CollectionUtils.isNotEmpty(filter.getOrganizations())) {
                        select.addWhere(makeVar(FacilityModel.INFRASTRUCTURE_FIELD), Oeso.isHosted, uriVar);
                        select.addFilter(SPARQLQueryHelper.inURIFilter(makeVar(FacilityModel.INFRASTRUCTURE_FIELD), filter.getOrganizations()));
                    }

                    if (!StringUtils.isEmpty(filter.getPattern())) {
                        select.addFilter(SPARQLQueryHelper.regexFilter(SiteModel.NAME_FIELD, filter.getPattern()));
                    }
                }),
                filter.getOrderByList(),
                filter.getPage(),
                filter.getPageSize()
        );

    }


    /**
     * Deletes a facility by URI. Checks that the user has access to it beforehand. See {@link #validateFacilityAccess(URI, UserModel)}
     * for further information on  access validation.
     *
     * @param uri The URI of the facility
     * @param user The current user
     * @throws Exception If the access is not validated, or if any other problem occurs
     */
    public void delete(URI uri, UserModel user) throws Exception {
        validateFacilityAccess(uri, user);

        FacilityModel model = sparql.getByURI(FacilityModel.class, uri, user.getLanguage());
        URI graphUri = sparql.getDefaultGraphURI(OrganizationModel.class);

        // Must delete the associated address if there is one
        if (model.getAddress() != null) {
            if (this.geospatialDAO.getGeometryByURI(uri, graphUri) != null) {
                this.geospatialDAO.delete(uri, graphUri);
            }

            sparql.delete(FacilityAddressModel.class, model.getAddress().getUri());
        }

        sparql.delete(FacilityModel.class, uri);
    }

    /**
     * Updates the facility. Checks that the user has access to it beforehand. See {@link #validateFacilityAccess(URI, UserModel)}
     * for further information on  access validation. Also checks that the address is valid, see {@link #validateFacilityAddress(FacilityModel, UserModel)}
     * for further information.
     *
     * @param instance the facility to update
     * @param user The current user
     * @return The facility
     * @throws Exception If the access is not validated, or if any other problem occurs
     */
    public FacilityModel update(FacilityModel instance, UserModel user) throws Exception {
        validateFacilityAccess(instance.getUri(), user);
        validateFacilityAddress(instance, user);

        List<OrganizationModel> organizationModels = sparql.getListByURIs(OrganizationModel.class, instance.getInfrastructureUris(), user.getLanguage());
        instance.setInfrastructures(organizationModels);

        URI graphUri = sparql.getDefaultGraphURI(OrganizationModel.class);

        FacilityModel existingModel = sparql.getByURI(FacilityModel.class, instance.getUri(), user.getLanguage());

        if (existingModel.getAddress() != null) {
            if (this.geospatialDAO.getGeometryByURI(instance.getUri(), graphUri) != null) {
                this.geospatialDAO.delete(instance.getUri(), graphUri);
            }

            sparql.delete(FacilityAddressModel.class, existingModel.getAddress().getUri());
        }

        createFacilityGeospatialModel(instance);

        sparql.update(instance);
        return instance;
    }

    /**
     * Gets the geospatial model corresponding to the given facility. There is no access check in this method, so please
     * make sure that the user has access to the given facility (by calling {@link #get(URI, UserModel)}, for example.
     *
     * @param facilityUri The URI of the facility
     * @return The geospatial model
     */
    public GeospatialModel getFacilityGeospatialModel(URI facilityUri) {
        return geospatialDAO.getGeometryByURI(facilityUri, addressGraphURI);
    }

    private void createFacilityGeospatialModel(FacilityModel facility) {
        if (facility.getAddress() == null) {
            return;
        }

        FacilityAddressDTO addressDto = new FacilityAddressDTO();
        addressDto.fromModel(facility.getAddress());

        Geometry geom = geocodingService.getPointFromAddress(addressDto.toReadableAddress());

        if (geom == null) {
            return;
        }

        GeospatialModel geospatialModel = new GeospatialModel();
        geospatialModel.setUri(facility.getUri());
        geospatialModel.setName(facility.getName());
        geospatialModel.setRdfType(facility.getType());
        geospatialModel.setGraph(addressGraphURI);
        geospatialModel.setGeometry(geom);

        this.geospatialDAO.create(geospatialModel);
    }

    /**
     * Validates that the user has access to a facility. Throws an exception if that is not the case. The
     * facility must exist, and the user/facility must satisfy at least one of the following conditions :
     *
     * <ul>
     *     <li>The user is admin</li>
     *     <li>The user has access to an organization hosted by the facility</li>
     *     <li>The user has access to a site containing the facility</li>
     *     <li>The user is the creator of the facility</li>
     * </ul>
     *
     * @param facilityURI The facility URI to check
     * @param userModel The user
     * @throws IllegalArgumentException If the userModel is null
     */
    private void validateFacilityAccess(URI facilityURI, UserModel userModel) throws Exception {
        if (Objects.isNull(userModel)) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (!sparql.uriExists(FacilityModel.class, facilityURI)) {
            throw new NotFoundURIException(facilityURI);
        }

        if (userModel.isAdmin()) {
            return;
        }

        final List<URI> userOrganizations = organizationDAO.search(null, null, userModel)
                        .stream().map(SPARQLResourceModel::getUri)
                        .collect(Collectors.toList());

        final List<URI> userSites = siteDAO.search(new SiteSearchFilter()
                            .setUser(userModel)
                            .setUserOrganizations(userOrganizations)
                            .setSkipUserOrganizationFetch(true))
                        .getList().stream().map(SPARQLResourceModel::getUri)
                        .collect(Collectors.toList());

        Var uriVar = makeVar(FacilityModel.URI_FIELD);

        AskBuilder ask = new AskBuilder();
        ask.addWhere(uriVar, Ontology.typeSubClassAny, sparql.getMapperIndex().getForClass(FacilityModel.class).getRDFType());
        ask.addFilter(SPARQLQueryHelper.eq(uriVar, SPARQLDeserializers.nodeURI(facilityURI)));
        organizationSPARQLHelper.addFacilityAccessClause(ask, uriVar, userOrganizations, userSites, userModel.getUri());

        if (!sparql.executeAskQuery(ask)) {
            throw new ForbiddenURIAccessException(facilityURI);
        }
    }

    /**
     * Checks if a facility address is valid (it must be the same as its sites).
     *
     * @param facilityModel The facility
     * @param user The user
     * @throws SiteFacilityInvalidAddressException If the address is invalid
     */
    protected void validateFacilityAddress(FacilityModel facilityModel, UserModel user) throws Exception {
        if (facilityModel.getUri() == null) {
            return;
        }

        if (facilityModel.getAddress() == null) {
            return;
        }

        List<SiteModel> siteModelList = this.siteDAO.getByFacility(facilityModel.getUri(), user);

        for (SiteModel siteModel : siteModelList) {
            if (siteModel.getAddress() != null) {
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
}
