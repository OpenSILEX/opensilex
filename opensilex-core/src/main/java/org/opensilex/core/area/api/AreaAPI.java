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
import io.swagger.annotations.*;
import org.opensilex.core.area.dal.AreaDAO;
import org.opensilex.core.area.dal.AreaModel;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.nosql.datanucleus.DataNucleusService;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.SingleObjectResponse;
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
            @ApiResponse(code = 200, message = "Create a area", response = ObjectUriResponse.class),
            @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class),
            @ApiResponse(code = 409, message = "An area with the same URI already exists", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })

    public Response createArea(
            @org.jetbrains.annotations.NotNull @ApiParam("Area description") @Valid AreaCreationDTO dto
    ) throws Exception {
        AreaDAO dao = new AreaDAO(sparql);
        GeospatialDAO geoDAO = new GeospatialDAO(nosql);

        ClientSession session = nosql.getMongoDBClient().startSession();
        session.startTransaction();
        try {
            sparql.startTransaction();
            URI areaURI = dao.create(dto.getName(), dto.getRdfType(), dto.getDescription());

            GeospatialModel geospatialModel = new GeospatialModel();
            geospatialModel.setUri(areaURI);
            geospatialModel.setType(dto.getRdfType());
            geospatialModel.setGeometry(GeospatialDAO.geoJsonToGeometry(dto.getGeometry()));
            geoDAO.create(geospatialModel);

            sparql.commitTransaction();
            session.commitTransaction();

            return new ObjectUriResponse(areaURI).getResponse();
        } catch (Exception ex) {
            sparql.rollbackTransaction();
            session.abortTransaction();
            throw ex;
        }
    }

    /**
     * @param uri of the area
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
        GeospatialDAO geoDAO = new GeospatialDAO(nosql);

        AreaModel model = areaDAO.getByURI(uri);
        GeospatialModel geometryByURI = geoDAO.getGeometryByURI(uri, null);

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

    /**
     * @param name zone name
     * @param type Area Type : Area, WindyArea, etc
     * @param page Page number
     * @param pageSize Page size
     * @return the result of the search by intersection
     * @throws Exception in case of a problem in the area search
     */
//    @GET
//    @Path("search")
//    @ApiOperation("Search Area")
//    @ApiProtected
//    @ApiCredential(
//            credentialId = CREDENTIAL_AREA_READ_ID,
//            credentialLabelKey = CREDENTIAL_AREA_READ_LABEL_KEY
//    )
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "Return the list of zones by intersection", response = AreaSearchDTO.class, responseContainer = "List"),
//            @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
//    })
//    public Response searchAreaList(
//            @ApiParam(value = "Regex pattern for filtering list by name", example = ".*") @DefaultValue(".*") @QueryParam("name") String name,
//            @ApiParam(value = "Search by type", example = AREA_EXAMPLE_TYPE) @QueryParam("type") URI type,
////                                   @ApiParam(value = "Search by area intersection", example = AREA_EXAMPLE_GEOMETRY) @QueryParam("geometry") GeoJsonObject geometry,
//            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
//            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
//    ) throws Exception {
//        // Search area with area DAO
//        AreaDAO dao = new AreaDAO(sparql, nosql);
//        GeometryModel geometry = null;
//        ListWithPagination<AreaModel> resultList = dao.search(
//                currentUser,
//                name,
//                type,
//                geometry,
//                page,
//                pageSize
//        );
//
//        // Convert paginated list to DTO
//        ListWithPagination<AreaSearchDTO> resultDTOList = resultList.convert(
//                AreaSearchDTO.class,
//                AreaSearchDTO::fromModel
//        );
//
//        // Return paginated list of profiles DTO
//        return new PaginatedListResponse<>(resultDTOList).getResponse();
//    }
}
