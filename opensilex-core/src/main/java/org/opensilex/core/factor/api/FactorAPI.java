/*
 * ******************************************************************************
 *                                     FactorAPI.java
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
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opensilex.core.factor.dal.FactorDAO;
import org.opensilex.core.factor.dal.FactorModel;
import org.opensilex.rest.authentication.ApiCredential;
import org.opensilex.rest.authentication.ApiProtected;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 *
 * @author Arnaud Charleroy
 */
@Api(value = "Factors", hidden = true)
@Path("/core/factor")
public class FactorAPI {

    protected static final String FACTOR_EXAMPLE_URI = "http://opensilex/set/factors/ZA17";
    
    public static final String CREDENTIAL_FACTOR_GROUP_ID = "Factors";
    public static final String CREDENTIAL_FACTOR_GROUP_LABEL_KEY = "credential-groups.factors";

    public static final String CREDENTIAL_FACTOR_MODIFICATION_ID = "factor-modification";
    public static final String CREDENTIAL_FACTOR_MODIFICATION_LABEL_KEY = "credential.factor.modification";

    public static final String CREDENTIAL_FACTOR_READ_ID = "factor-read";
    public static final String CREDENTIAL_FACTOR_READ_LABEL_KEY = "credential.factor.read";

    public static final String CREDENTIAL_FACTOR_DELETE_ID = "factor-delete";
    public static final String CREDENTIAL_FACTOR_DELETE_LABEL_KEY = "credential.factor.delete";

    
    @Inject
    public FactorAPI(SPARQLService sparql) {
        this.sparql = sparql;
    }

    private final SPARQLService sparql;

    /**
     * Create a factor model from a FactorCreationDTO object
     *
     * @param dto FactorCreationDTO object
     * @return Factor URI
     * @throws Exception if creation failed
     */
    @POST
    @ApiOperation("Create an factor")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createFactor(
            @ApiParam("Factor description") @Valid FactorCreationDTO dto
    ) throws Exception {
        FactorDAO dao = new FactorDAO(sparql);
        try {
            FactorModel model = dto.newModel();
            dao.create(model);
            return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();
        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Factor already exists",
                    duplicateUriException.getMessage()
            ).getResponse();
        }
    }

    /**
     * Retreive factor by uri
     *
     * @param uri factor uri
     * @return Return factor detail
     * @throws Exception in case of server error
     */
    @GET
    @Path("{uri}")
    @ApiOperation("Get an factor")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFactor(
            @ApiParam(value = "Factor URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        FactorDAO dao = new FactorDAO(sparql);
        FactorModel model = dao.get(uri);

        if (model != null) {
            return new SingleObjectResponse<>(
                    FactorGetDTO.fromModel(model)
            ).getResponse();
        } else {
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Factor not found",
                    "Unknown factor URI: " + uri.toString()
            ).getResponse();
        }
    }

    /**
     * Search factors
     *
     * @see org.opensilex.core.factor.dal.FactorDAO
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
    @ApiOperation("Search factors")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return factor list", response = FactorGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response searchFactors(
            @ApiParam(value = "Regex pattern for filtering list by alias", example = "Irrigation") @QueryParam("alias") String alias,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "alias=asc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        
        FactorSearchDTO factorSearchDTO = new FactorSearchDTO();
        factorSearchDTO.setAlias(alias);
        // Search factors with Factor DAO
        FactorDAO dao = new FactorDAO(sparql);
        ListWithPagination<FactorModel> resultList = dao.search(
                factorSearchDTO,
                orderByList,
                page,
                pageSize
        );

        // Convert paginated list to DTO
        ListWithPagination<FactorGetDTO> resultDTOList = resultList.convert(
                FactorGetDTO.class,
                FactorGetDTO::fromModel
        );

        // Return paginated list of factor DTO
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }
 
    /**
     * Remove an factor
     *
     * @param xpUri the factor URI
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the deleted Factor {@link URI}
     */
    @DELETE
    @Path("delete/{uri}")
    @ApiOperation("Delete an factor")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_FACTOR_GROUP_ID,
            groupLabelKey = CREDENTIAL_FACTOR_GROUP_LABEL_KEY,
            credentialId = CREDENTIAL_FACTOR_DELETE_ID,
            credentialLabelKey = CREDENTIAL_FACTOR_DELETE_LABEL_KEY,
            hide = true
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Factor deleted", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Invalid or unknown Factor URI", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public Response deleteFactor(
            @ApiParam(value = "Factor URI", example = FACTOR_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI xpUri
    ) {
        try {
            FactorDAO dao = new FactorDAO(sparql);
            dao.delete(xpUri);
            return new ObjectUriResponse(xpUri).getResponse();

        } catch (SPARQLInvalidURIException e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "Invalid or unknown Factor URI", e.getMessage()).getResponse();
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }
}
