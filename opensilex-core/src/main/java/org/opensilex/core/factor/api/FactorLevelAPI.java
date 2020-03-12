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
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opensilex.core.factor.dal.FactorLevelDAO;
import org.opensilex.core.factor.dal.FactorLevelModel;
import org.opensilex.rest.authentication.ApiProtected;
import org.opensilex.rest.validation.ValidURI;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 *
 * @author Arnaud Charleroy
 */
//@Api("FactorLevels")
//@Path("/core/FactorLevels")
public class FactorLevelAPI {

    @Inject
    public FactorLevelAPI(SPARQLService sparql) {
        this.sparql = sparql;
    }

    private final SPARQLService sparql;

    /**
     * Create a factorLevel model from a FactorLevelCreationDTO object
     *
     * @param dto FactorLevelCreationDTO object
     * @return FactorLevel URI
     * @throws Exception if creation failed
     */
    @POST
    @ApiOperation("Create an factorLevel")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createFactorLevel(
            @ApiParam("FactorLevel description") @Valid FactorLevelCreationDTO dto
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
     * Retreive factorLevel by uri
     *
     * @param uri factorLevel uri
     * @return Return factorLevel detail
     * @throws Exception in case of server error
     */
    @GET
    @Path("{uri}")
    @ApiOperation("Get an factorLevel")
    @ApiProtected
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
     * @param factor Factor URI used to filter list
     * @see org.opensilex.core.factorLevel.dal.FactorLevelDAO
     * @param alias Regex pattern for filtering list by alias
     * @param orderByList List of fields to sort as an array of
     * fieldName=asc|desc
     * @param page Page number
     * @param pageSize Page size
     * @return filtered, ordered and paginated list
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @GET
    @Path("search")
    @ApiOperation("Search factorLevels")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return factorLevel list", response = FactorLevelGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response searchFactorLevels(
            @ApiParam(value = "Regex pattern for filtering list by alias", example = "Well watered") @QueryParam("alias") String hasAlias,
            @ApiParam(value = "Factor URI used to filter list", example = "Irrigation") @QueryParam("factor") URI hasFactor,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "alias=asc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        
        FactorLevelSearchDTO factorLevelSearchDTO = new FactorLevelSearchDTO();
        factorLevelSearchDTO.setAlias(hasAlias);
        factorLevelSearchDTO.setHasFactor(hasFactor);
        // Search factorLevels with FactorLevel DAO
        FactorLevelDAO dao = new FactorLevelDAO(sparql);
        ListWithPagination<FactorLevelModel> resultList = dao.search(
                factorLevelSearchDTO,
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
     * Remove a factorLevel
     *
     * @param uri factorLevel uri
     * @return URI of deleted factorLevel
     * @throws Exception if suppression failed
     */
    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete a user")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteFactorLevel(
            @ApiParam(value = "FactorLevel URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull @ValidURI URI uri
    ) throws Exception {
        FactorLevelDAO dao = new FactorLevelDAO(sparql);
        dao.delete(uri);
        return new ObjectUriResponse(Response.Status.OK, uri).getResponse();
    }
}
