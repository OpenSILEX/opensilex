/*
 * *******************************************************************************
 *                     AreaAPI.java
 * OpenSILEX
 * Copyright © INRAE 2020
 * Creation date: September 14, 2020
 * Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * *******************************************************************************
 */
package org.opensilex.core.area.api;

import com.mongodb.MongoQueryException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import io.swagger.annotations.*;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.geojson.GeoJsonObject;
import org.opensilex.core.area.dal.AreaDAO;
import org.opensilex.core.area.dal.AreaModel;
import org.opensilex.core.event.dal.EventDAO;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.event.dal.EventSearchFilter;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.*;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.server.rest.validation.date.ValidOffsetDateTime;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.response.CreatedUriResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.opensilex.core.geospatial.dal.GeospatialDAO.geoJsonToGeometry;

/**
 * Area API
 *
 * @author Jean Philippe VERT
 */
@Api(AreaAPI.CREDENTIAL_AREA_GROUP_ID)
@Path("/core/area")
@ApiCredentialGroup(
        groupId = AreaAPI.CREDENTIAL_AREA_GROUP_ID,
        groupLabelKey = AreaAPI.CREDENTIAL_AREA_GROUP_LABEL_KEY
)
public class AreaAPI {

    public static final String CREDENTIAL_AREA_GROUP_ID = "Area";
    public static final String CREDENTIAL_AREA_GROUP_LABEL_KEY = "credential-groups.area";

    public static final String CREDENTIAL_AREA_MODIFICATION_ID = "area-modification";
    public static final String CREDENTIAL_AREA_MODIFICATION_LABEL_KEY = "credential.default.modification";

    public static final String CREDENTIAL_AREA_DELETE_LABEL_KEY = "credential.default.delete";
    public static final String INVALID_GEOMETRY = "Invalid geometry (longitude must be between -180 and 180 and latitude must be between -90 and 90, no self-intersection, ...)";
    private static final String CREDENTIAL_AREA_DELETE_ID = "area-delete";

    @CurrentUser
    AccountModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    /**
     * Create an Area
     *
     * @param areaDTO the Area to create
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the created Area {@link URI}
     * @throws java.lang.Exception if creation failed
     */
    @POST
    @ApiOperation("Add an area")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_AREA_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_AREA_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Add an area", response = URI.class),
        @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
        @ApiResponse(code = 409, message = "An area with the same URI already exists", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })

    public Response createArea(
            @ApiParam("Area description") @NotNull @Valid AreaCreationDTO areaDTO
    ) throws Exception {
        AreaDAO dao = new AreaDAO(sparql);
        GeospatialDAO geoDAO = new GeospatialDAO(nosql);
        EventDAO<EventModel> eventDAO= new EventDAO<>(sparql,nosql);
        GeospatialModel geospatialModel = new GeospatialModel();
        URI areaURI;

        nosql.startTransaction();
        sparql.startTransaction();
        try {
            if (!areaDTO.isStructuralArea){

                //RdfType for area and geospatial is equal temporal area
                URI temporalArea = new URI(Oeso.TemporalArea.toString());

                areaURI = dao.create(areaDTO.getUri(), areaDTO.getName(), temporalArea, areaDTO.getDescription(), currentUser.getUri());
                geospatialModel.setRdfType(temporalArea);

                //TODO : when the eventForm is used in the area context, the rdftype "move" is disabled because don't make sense (temporary until creation of specific service)
                //Move is a specific service distinct from the event, not linked
                if(SPARQLDeserializers.compareURIs(areaDTO.getRdfType(), Oeev.Move.getURI())){
                    throw new IllegalArgumentException("The rdf Type 'Move' in the context Area is not managed");
                }
                else{
                    //Create an event with the rdfType from event
                    areaDTO.event.setTargets(Arrays.asList(areaURI));
                    eventDAO.create(areaDTO.event.toModel());
                }
            }
            else{
                areaURI = dao.create(areaDTO.getUri(), areaDTO.getName(),areaDTO.getRdfType(), areaDTO.getDescription(), currentUser.getUri());
                geospatialModel.setRdfType(areaDTO.getRdfType());
            }

            geospatialModel.setUri(areaURI);
            geospatialModel.setName(areaDTO.getName());
            geospatialModel.setGeometry(geoJsonToGeometry(areaDTO.getGeometry()));
            geoDAO.create(geospatialModel);

            sparql.commitTransaction();
            nosql.commitTransaction();

            return new CreatedUriResponse(areaURI).getResponse();
        } catch (MongoWriteException | CodecConfigurationException mongoException) {
            try {
                sparql.rollbackTransaction(mongoException);
                nosql.rollbackTransaction();
            } catch (Exception e) {
                return new ErrorResponse(Response.Status.BAD_REQUEST, INVALID_GEOMETRY, mongoException).getResponse();
            }
            throw mongoException;
        } catch (Exception ex) {
            sparql.rollbackTransaction(ex);
            nosql.rollbackTransaction();
            throw ex;
        }
    }

    /**
     * @param  areaURI uri of the area
     * @return all the data associated with the area
     * @throws Exception the data is non-compliant or the uri already existing
     */
    @GET
    @Path("{uri}")
    @ApiOperation("Get an area")
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return area", response = AreaGetDTO.class),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Area not found", response = ErrorDTO.class)
    })
    public Response getByURI(
            @ApiParam(value = "area URI", required = true) @PathParam("uri") @NotNull URI areaURI
    ) throws Exception {
        // Get area, its geospatial and if it's a temporal area, its event by URI
        AreaDAO areaDAO = new AreaDAO(sparql);
        GeospatialDAO geoDAO = new GeospatialDAO(nosql);
        EventDAO<EventModel> eventDAO= new EventDAO<>(sparql,nosql);

        AreaModel model = areaDAO.getByURI(areaURI);
        GeospatialModel geometryByURI = geoDAO.getGeometryByURI(areaURI, null);

        // Check if area is found
        if (model != null) {
            //create search filter
            EventSearchFilter searchFilter = new EventSearchFilter();
            searchFilter.setTarget(areaURI.toString())
                    .setLang(currentUser.getLanguage());

            // Check if an event is linked to the area
            ListWithPagination<EventModel> eventList = eventDAO.search(searchFilter);


            switch (eventList.getList().size()) {
                case 1:
                    EventModel eventByURI = eventDAO.get(eventList.getList().get(0).getUri(), currentUser);
                    return new SingleObjectResponse<>(AreaGetDTO.fromModel(model, geometryByURI, eventByURI)).getResponse();
                case 0:
                    return new SingleObjectResponse<>(AreaGetDTO.fromModel(model, geometryByURI)).getResponse();
                default:
                    return new ErrorResponse(
                            Response.Status.UNAUTHORIZED,
                            "Number of associated events",
                            "More than 1 event associated to this area : " + areaURI.toString()
                    ).getResponse();
            }
        } else {
            // Otherwise return a 404 - NOT_FOUND error response
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Area not found",
                    "Unknown area URI: " + areaURI.toString()
            ).getResponse();
        }
    }

    /**
     * @param areaDTO the Area to create
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the created Area {@link URI}
     * @throws java.lang.Exception if creation failed
     */
    @PUT
    @ApiOperation("Update an area")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_AREA_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_AREA_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Update an area", response = URI.class)
    })
    public Response updateArea(
            @ApiParam(value = "Area description", required = true) @NotNull @Valid AreaUpdateDTO areaDTO
    ) throws Exception {

        AreaDAO dao = new AreaDAO(sparql);
        GeospatialDAO geoDAO = new GeospatialDAO(nosql);
        EventDAO<EventModel> eventDAO= new EventDAO<>(sparql,nosql);
        GeospatialModel geospatialModel = new GeospatialModel();
        URI areaURI;

        nosql.startTransaction();
        sparql.startTransaction();
        try {
            //create search filter
            EventSearchFilter searchFilter = new EventSearchFilter();
            searchFilter.setTarget(areaDTO.getUri().toString())
                    .setLang(currentUser.getLanguage());

            // Check if an event is linked to the area
            ListWithPagination<EventModel> eventList = eventDAO.search(searchFilter);

             if(!areaDTO.isStructuralArea){
                //RdfType for area and geospatial is equal temporal area
                URI temporalArea = new URI(Oeso.TemporalArea.toString());
                areaURI = dao.update(areaDTO.getUri(), areaDTO.getName(), temporalArea , areaDTO.getDescription(), currentUser.getUri());
                geospatialModel.setRdfType(temporalArea);

                 //If linked event exist, update it
                 switch (eventList.getList().size()){
                     case 1:
                         areaDTO.event.setUri(eventList.getList().get(0).getUri());
                         areaDTO.event.setTargets(Arrays.asList(areaURI));
                         eventDAO.update(areaDTO.event.toModel());
                         break;
                    case 0: throw new IllegalArgumentException("No event to update for this area : " + areaURI);
                    default: throw new IllegalArgumentException("More than 1 event for this area : " + areaURI);
                 }
            }
            else {
                 areaURI = dao.update(areaDTO.getUri(), areaDTO.getName(), areaDTO.getRdfType(), areaDTO.getDescription(), currentUser.getUri());
                 geospatialModel.setRdfType(areaDTO.getRdfType());
            }
            geospatialModel.setUri(areaURI);
            geospatialModel.setGeometry(geoJsonToGeometry(areaDTO.getGeometry()));
            geospatialModel.setName(areaDTO.getName());
            geoDAO.update(geospatialModel,areaURI,null);

            sparql.commitTransaction();
            nosql.commitTransaction();

            return new ObjectUriResponse(areaURI).getResponse();
        } catch (MongoWriteException | CodecConfigurationException mongoException) {
            try {
                sparql.rollbackTransaction(mongoException);
                nosql.rollbackTransaction();
            } catch (Exception e) {
                return new ErrorResponse(Response.Status.BAD_REQUEST, INVALID_GEOMETRY, mongoException).getResponse();
            }
            throw mongoException;
        } catch (Exception ex) {
            sparql.rollbackTransaction();
            nosql.rollbackTransaction();
            throw ex;
        }
    }

    /**
     * @param areaURI uri of the area
     * @return the result of the deletion
     * @throws Exception if deletion fails
     */
    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete an area")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_AREA_DELETE_ID,
            credentialLabelKey = CREDENTIAL_AREA_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Delete an area", response = URI.class),
        @ApiResponse(code = 404, message = "The URI for the area was not found.", response = ErrorResponse.class)
    })
    public Response deleteArea(
            @ApiParam(value = "Area URI", required = true) @PathParam("uri") @NotNull @ValidURI URI areaURI
    ) throws Exception {
        AreaDAO dao = new AreaDAO(sparql);
        GeospatialDAO geoDAO = new GeospatialDAO(nosql);
        EventDAO<EventModel> eventDAO= new EventDAO<>(sparql,nosql);

        nosql.startTransaction();
        sparql.startTransaction();
        try {
            dao.delete(areaURI);
            geoDAO.delete(areaURI, null);

            //create search filter
            EventSearchFilter searchFilter = new EventSearchFilter();
            searchFilter.setTarget(areaURI.toString())
                    .setLang(currentUser.getLanguage());

            //search if an event is linked
            ListWithPagination<EventModel> eventList = eventDAO.search(searchFilter);

            //If linked event exist, delete it
            switch (eventList.getList().size()){
                case 1:
                    eventDAO.delete(eventList.getList().get(0).getUri());
                    break;
                case 0: break;
                default: throw new IllegalArgumentException("More than 1 event for this area : " + areaURI);
            }

            sparql.commitTransaction();
            nosql.commitTransaction();

            return new ObjectUriResponse(Response.Status.OK, areaURI).getResponse();
        } catch (Exception ex) {
            sparql.rollbackTransaction();
            nosql.rollbackTransaction();
            throw ex;
        }
    }

    /**
     * @param geometry geometry of the area
     * @param start Start date : temporal area event after the given start date
     * @param end End date : temporal area match event before the given end date
     * @return Return list of area whose geometry corresponds to the
     * Intersections
     * @throws Exception the data is non-compliant or the uri already existing
     */
    @POST
    @Path("intersects")
    @ApiOperation("Get area whose geometry corresponds to the Intersections")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Get area whose geometry corresponds to the Intersections", response = AreaGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Area not found", response = ErrorDTO.class)
    })
    public Response searchIntersects(
            @ApiParam(value = "geometry GeoJSON", required = true) @NotNull GeoJsonObject geometry,
            @ApiParam(value = "Start date : match temporal area after the given start date", example = "2019-09-08T12:00:00+01:00") @QueryParam("start") @ValidOffsetDateTime String start,
            @ApiParam(value = "End date : match temporal area before the given end date", example = "2021-09-08T12:00:00+01:00") @QueryParam("end") @ValidOffsetDateTime String end
    ) throws Exception {
        GeospatialDAO geoDAO = new GeospatialDAO(nosql);
        EventDAO<EventModel> eventDAO= new EventDAO<>(sparql,nosql);
        AreaDAO areaDAO = new AreaDAO(sparql);
        List<AreaGetDTO> dtoList = new ArrayList<>();

        // Get geospatial List
        FindIterable<GeospatialModel> mapGeo = geoDAO.searchIntersectsArea(geoJsonToGeometry(geometry), currentUser, sparql);

        List<URI> areaURIList = new ArrayList<>();
        // TODO: search with date : to upgrade
        /*List<GeospatialModel> mapGeoTmp = new ArrayList<>();
        if (start != null || end != null) {
            // Extract URI List
            List<URI> temporalAreasUris = new ArrayList<>();
            List<URI> otherAreasUris = new ArrayList<>();
            
            for (GeospatialModel geospatialModel : mapGeo) {
                URI uri = geospatialModel.getUri();
                if (SPARQLDeserializers.getExpandedURI(geospatialModel.getRdfType().toString())
                        .equals(SPARQLDeserializers.getExpandedURI(Oeso.TemporalArea.toString()))) {
                    temporalAreasUris.add(new URI(SPARQLDeserializers.getExpandedURI(uri.toString())));
                } else {
                    otherAreasUris.add(new URI(SPARQLDeserializers.getExpandedURI(uri.toString())));
                }
            }

            // Search Event by URI values  List
            ListWithPagination<EventModel> search = eventDAO.search(
                    null,
                    temporalAreasUris,
                    null,
                    null,
                    start != null ? OffsetDateTime.parse(start) : null,
                    end != null ? OffsetDateTime.parse(end) : null,
                    null,
                    null,
                    null,
                    null);
            List<URI> foundedTemporalAreaUris = new ArrayList<>();
            // list targets
            for (EventModel eventModel : search.getList()) {
                foundedTemporalAreaUris.addAll(eventModel.getTargets());
            }

            // List unique targets
            List<URI> uniqueAreaUris;

            uniqueAreaUris = foundedTemporalAreaUris.stream().distinct()
                    .map(uri -> {
                        try {
                            return new URI(SPARQLDeserializers.getExpandedURI(uri.toString()));
                        } catch (URISyntaxException ex) {
                            return uri;
                        }
                    })
                    .collect(Collectors.toList());

            for (GeospatialModel geospatialModel : mapGeo) {
                if (otherAreasUris.contains(new URI(SPARQLDeserializers.getExpandedURI(geospatialModel.getUri().toString())))) {
                    mapGeoTmp.add(geospatialModel);
                } else {
                    if (uniqueAreaUris.contains(new URI(SPARQLDeserializers.getExpandedURI(geospatialModel.getUri().toString())))) {
                        mapGeoTmp.add(geospatialModel);
                    }
                }
            }
        }
        if (start != null || end != null) {
            for (GeospatialModel geospatialModel : mapGeoTmp) {
                AreaGetDTO dtoFromModel = AreaGetDTO.fromModel(geospatialModel);
                dtoList.add(dtoFromModel);
            }
        }*/

        try {
            //get area URI list from geometry list
            for (GeospatialModel geospatialModel: mapGeo) {
                areaURIList.add(geospatialModel.getUri());
            }
            //search all areas
            List<AreaModel> areaModelList = areaDAO.searchByURIs(areaURIList,currentUser);
            //search all events with the target URI list and a type = area
            EventSearchFilter searchFilter = new EventSearchFilter();
            searchFilter.setTargets(areaURIList)
                    .setBaseType(new URI(SPARQLDeserializers.getShortURI(Oeso.Area.getURI())));

            ListWithPagination<EventModel> eventModelList = eventDAO.search(searchFilter);

            for (GeospatialModel geospatialModel : mapGeo) {
                String geospatial = geospatialModel.getUri().toString();
                //Get the corresponding area
                AreaModel areaModel = areaModelList.stream()
                        .filter(findArea -> geospatial.equals(findArea.getUri().toString()))
                        .reduce((a,b) -> {
                            throw new IllegalStateException("Multiple elements: " + a + ", " + b);
                        })
                        .get();
                // Get the corresponding event?
                EventModel eventModel = eventModelList.getList().stream()
                        .filter(findEvent -> geospatial.equals(findEvent.getTargets().get(0).toString()))
                        .reduce((a,b) -> {
                            throw new IllegalStateException("Multiple elements: " + a + ", " + b);
                        })
                        .orElse(null);
                if(eventModel != null){
                    //Add 3 Models
                    dtoList.add(AreaGetDTO.fromModel(areaModel,geospatialModel, eventModel));
                }
                else{
                    //Add 2 Models
                    dtoList.add(AreaGetDTO.fromModel(areaModel,geospatialModel));
                }
            }
        } catch (MongoQueryException mongoException) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, INVALID_GEOMETRY, mongoException).getResponse();
        }
        return new PaginatedListResponse<>(dtoList).getResponse();
    }
}
