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

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import io.swagger.annotations.*;
import org.opensilex.core.area.dal.AreaDAO;
import org.opensilex.core.area.dal.AreaModel;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.nosql.datanucleus.DataNucleusService;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

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

    public static final String CREDENTIAL_AREA_READ_ID = "area-read";
    public static final String CREDENTIAL_AREA_READ_LABEL_KEY = "credential.area.read";

    public static final String CREDENTIAL_AREA_MODIFICATION_ID = "area-modification";
    public static final String CREDENTIAL_AREA_MODIFICATION_LABEL_KEY = "credential.area.modification";

    public static final String CREDENTIAL_AREA_DELETE_LABEL_KEY = "credential.area.delete";
    private static final String CREDENTIAL_AREA_DELETE_ID = "area-delete";

    @CurrentUser
    UserModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private DataNucleusService nosql;

    /**
     * Create a Area
     *
     * @param dto the Area to create
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the created Area {@link URI}
     * @throws java.lang.Exception if creation failed
     */
    @POST
    @Path("create")
    @ApiOperation("Create a Area")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_AREA_READ_ID,
            credentialLabelKey = CREDENTIAL_AREA_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Create a area", response = ObjectUriResponse.class),
            @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
            @ApiResponse(code = 409, message = "An area with the same URI already exists", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })

    public Response createArea(
            @ApiParam("Area description") @NotNull @Valid AreaCreationDTO dto
    ) throws Exception {
        AreaDAO dao = new AreaDAO(sparql);

        MongoClient mongoClient = nosql.getMongoDBClient();
        GeospatialDAO geoDAO = new GeospatialDAO(mongoClient);

        ClientSession session = mongoClient.startSession();
        session.startTransaction();

        try {
            sparql.startTransaction();
            URI areaURI = dao.create(dto.getUri(), dto.getName(), dto.getType(), dto.getDescription(), currentUser.getUri());

            GeospatialModel geospatialModel = new GeospatialModel();
            geospatialModel.setUri(areaURI);
            geospatialModel.setType(dto.getType());
            geospatialModel.setGeometry(GeospatialDAO.geoJsonToGeometry(dto.getGeometry()));
            geoDAO.create(geospatialModel);

            sparql.commitTransaction();
            session.commitTransaction();

            return new ObjectUriResponse(Response.Status.CREATED, areaURI).getResponse();
        } catch (Exception ex) {
            sparql.rollbackTransaction(ex);
            session.abortTransaction();
            throw ex;
        } finally {
            mongoClient.close();
        }
    }

    /**
     * @param uri uri of the area
     * @return all the data associated with the area
     * @throws Exception the data is non-compliant or the uri already existing
     */
    @GET
    @Path("get/{uri}")
    @ApiOperation("Get a area by its URI")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_AREA_READ_ID,
            credentialLabelKey = CREDENTIAL_AREA_READ_LABEL_KEY
    )
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return profile", response = AreaGetSingleDTO.class),
            @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
            @ApiResponse(code = 404, message = "Area not found", response = ErrorDTO.class)
    })
    public Response getByURI(
            @ApiParam(value = "area URI", example = "http://opensilex.dev/set/area#z_004", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        // Get area from DAO by URI
        AreaDAO areaDAO = new AreaDAO(sparql);

        MongoClient mongoClient = nosql.getMongoDBClient();
        GeospatialDAO geoDAO = new GeospatialDAO(mongoClient);

        AreaModel model = areaDAO.getByURI(uri);
        GeospatialModel geometryByURI = geoDAO.getGeometryByURI(uri, null);

        mongoClient.close();
        // Check if area is found
        if (model != null) {
            return new SingleObjectResponse<>(AreaGetSingleDTO.fromModel(model, geometryByURI)).getResponse();
        } else {
            // Otherwise return a 404 - NOT_FOUND error response
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Area not found",
                    "Unknown area URI: " + uri.toString()
            ).getResponse();
        }
    }

    @PUT
    @Path("update")
    @ApiOperation("Update a area")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_AREA_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_AREA_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Area updated", response = ObjectUriResponse.class)
    })
    public Response updateArea(
            @ApiParam(value = "Area description", required = true) @NotNull @Valid AreaCreationDTO areaDTO
    ) throws Exception {

        AreaDAO dao = new AreaDAO(sparql);

        MongoClient mongoClient = nosql.getMongoDBClient();
        GeospatialDAO geoDAO = new GeospatialDAO(mongoClient);

        ClientSession session = mongoClient.startSession();
        session.startTransaction();
        try {
            sparql.startTransaction();
            URI areaURI = dao.update(areaDTO.getUri(), areaDTO.getName(), areaDTO.getType(), areaDTO.getDescription(), currentUser.getUri());

            GeospatialModel geospatialModel = new GeospatialModel();
            geospatialModel.setUri(areaURI);
            geospatialModel.setType(areaDTO.getType());
            geospatialModel.setGeometry(GeospatialDAO.geoJsonToGeometry(areaDTO.getGeometry()));
            geoDAO.update(geospatialModel, areaURI, null);

            sparql.commitTransaction();
            session.commitTransaction();

            mongoClient.close();

            return new ObjectUriResponse(areaURI).getResponse();
        } catch (Exception ex) {
            sparql.rollbackTransaction();
            session.abortTransaction();
            throw ex;
        }
    }

    /**
     * @param areaURI uri of the area
     * @return the result of the deletion
     * @throws Exception if deletion fails
     */
    @DELETE
    @Path("delete/{uri}")
    @ApiOperation("Delete a area")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_AREA_DELETE_ID,
            credentialLabelKey = CREDENTIAL_AREA_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deleting the area", response = ObjectUriResponse.class),
            @ApiResponse(code = 404, message = "The URI for the area was not found.", response = ErrorResponse.class)
    })
    public Response deleteArea(
            @ApiParam(value = "Area URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull @ValidURI URI areaURI
    ) throws Exception {
        AreaDAO dao = new AreaDAO(sparql);

        MongoClient mongoClient = nosql.getMongoDBClient();
        GeospatialDAO geoDAO = new GeospatialDAO(mongoClient);

        ClientSession session = mongoClient.startSession();
        session.startTransaction();
        try {
            sparql.startTransaction();
            dao.delete(areaURI);
            geoDAO.delete(areaURI, null);

            sparql.commitTransaction();
            session.commitTransaction();

            return new ObjectUriResponse(Response.Status.OK, areaURI).getResponse();
        } catch (Exception ex) {
            sparql.rollbackTransaction();
            session.abortTransaction();
            throw ex;
        } finally {
            mongoClient.close();
        }
    }
}