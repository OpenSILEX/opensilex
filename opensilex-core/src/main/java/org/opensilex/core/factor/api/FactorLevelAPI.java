/*
 * ******************************************************************************
 *                                     FactorLevelAPI.java
 *  OpenSILEX
 *  Copyright © INRA 2019
 *  Creation date:  17 December, 2019
 *  Contact: arnaud.charleroy@inra.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.factor.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.util.List;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opensilex.core.factor.dal.FactorLevelDAO;
import org.opensilex.core.factor.dal.FactorLevelModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 *
 * @author Arnaud Charleroy
 */
@Api("FactorLevels")
@Path("/core/FactorLevels")
@ApiCredentialGroup(
        groupId = FactorLevelAPI.CREDENTIAL_FACTOR_LEVEL_GROUP_ID,
        groupLabelKey = FactorLevelAPI.CREDENTIAL_FACTOR_LEVEL_GROUP_LABEL_KEY
)
public class FactorLevelAPI {
    
    public static final String CREDENTIAL_FACTOR_LEVEL_GROUP_ID = "Factors";
    public static final String CREDENTIAL_FACTOR_LEVEL_GROUP_LABEL_KEY = "credential-groups.factors";

    public static final String CREDENTIAL_FACTOR_LEVEL_MODIFICATION_ID = "factorLevels-modification";
    public static final String CREDENTIAL_FACTOR_LEVEL_MODIFICATION_LABEL_KEY = "credential.factorLevels.modification";

    public static final String CREDENTIAL_FACTOR_LEVEL_READ_ID = "factorLevels-read";
    public static final String CREDENTIAL_FACTOR_LEVEL_READ_LABEL_KEY = "credential.factorLevels.read";

    public static final String CREDENTIAL_FACTOR_LEVEL_DELETE_ID = "factorLevels-delete";
    public static final String CREDENTIAL_FACTOR_LEVEL_DELETE_LABEL_KEY = "credential.factorLevels.delete";

    @Inject
    private SPARQLService sparql;

    /**
     * Create a factor Level model from a FactorLevelCreationDTO object
     *
     * @param dto FactorLevelCreationDTO object
     * @return FactorLevel URI
     * @throws Exception if creation failed
     */
    @POST
    @ApiOperation("Create an factor level")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
        @ApiCredential(
            credentialId = CREDENTIAL_FACTOR_LEVEL_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_FACTOR_LEVEL_MODIFICATION_LABEL_KEY
    )
    public Response createFactorLevel(
            @ApiParam("Factor level description") @Valid FactorLevelCreationDTO dto
    ) throws Exception {
        FactorLevelDAO dao = new FactorLevelDAO(sparql);
        try {
            FactorLevelModel model = dto.newModel();
            dao.create(model);
            return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();
        } catch (SPARQLAlreadyExistingUriException e) {
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "FactorLevel already exists",
                    e.getMessage()
            ).getResponse();
        }
    }

    /**
     * Retreive factor Level by uri
     *
     * @param uri factorLevel uri
     * @return Return factorLevel detail
     * @throws Exception in case of server error
     */
    @GET
    @Path("{uri}")
    @ApiOperation("Get an factor level")
    @ApiProtected
        @ApiCredential(
            credentialId = CREDENTIAL_FACTOR_LEVEL_READ_ID,
            credentialLabelKey = CREDENTIAL_FACTOR_LEVEL_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFactorLevel(
            @ApiParam(value = "FactorLevel URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        FactorLevelDAO dao = new FactorLevelDAO(sparql);
        FactorLevelModel model = dao.get(uri);

        if (model != null) {
            return new SingleObjectResponse<>(
                    FactorLevelGetDTO.fromModel(model)
            ).getResponse();
        } else {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "FactorLevel not found",
                    "Unknown factorLevel URI: " + uri.toString()
            ).getResponse();
        }
    }

    /**
     * Search factorLevels
     *
     * @param factorLevelSearchDTO FactorLevel search form
     * @see org.opensilex.core.factorLevel.dal.FactorLevelDAO
     * @param orderByList List of fields to sort as an array of
     * fieldName=asc|desc
     * @param page Page number
     * @param pageSize Page size
     * @return filtered, ordered and paginated list
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @POST
    @Path("search")
    @ApiOperation("Search factor levels")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_FACTOR_LEVEL_READ_ID,
            credentialLabelKey = CREDENTIAL_FACTOR_LEVEL_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return factorLevel list", response = FactorLevelGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response searchFactorLevels(
            @ApiParam("FactorLevel search form") FactorLevelSearchDTO factorLevelSearchDTO,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "alias=asc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        
        
        // Search factorLevels with FactorLevel DAO
        FactorLevelDAO dao = new FactorLevelDAO(sparql);
        ListWithPagination<FactorLevelModel> resultList = dao.search(
                factorLevelSearchDTO.getAlias(),
                factorLevelSearchDTO.getHasFactor(),
                orderByList,
                page,
                pageSize
        );

        // Convert paginated list to DTO
        ListWithPagination<FactorLevelGetDTO> resultDTOList = resultList.convert(
                FactorLevelGetDTO.class,
                FactorLevelGetDTO::fromModel
        );

        // Return paginated list of factorLevel DTO
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    /**
     * Remove a factor level
     *
     * @param uri factor level uri
     * @return URI of deleted factorLevel
     * @throws Exception if suppression failed
     */
    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a factor level")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_FACTOR_LEVEL_DELETE_ID,
            credentialLabelKey = CREDENTIAL_FACTOR_LEVEL_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteFactorLevel(
            @ApiParam(value = "Factor level URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull @ValidURI URI uri
    ) throws Exception {
        FactorLevelDAO dao = new FactorLevelDAO(sparql);
        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }
    
    /**
     * @param factorLevelDTO the factor level to update
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the updated Factor level {@link URI}
     */
    @PUT
    @Path("update")
    @ApiOperation("Update a factor level")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_FACTOR_LEVEL_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_FACTOR_LEVEL_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "FactorLevel updated", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Invalid or unknown FactorLevel URI", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public Response updateFactorLevel(
            @ApiParam("Factor description") @Valid FactorLevelCreationDTO factorLevelDTO
    ) {
        try {
            FactorLevelDAO dao = new FactorLevelDAO(sparql);

            FactorLevelModel model = factorLevelDTO.newModel();
            dao.update(model);
            return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();

        } catch (SPARQLInvalidURIException e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "Invalid or unknown Factor Level URI", e.getMessage()).getResponse();
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }
}
