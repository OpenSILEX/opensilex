/*
 * ******************************************************************************
 *                                     FactorAPI.java
 *  OpenSILEX
 *  Copyright © INRA 2019
 *  Creation date:  17 December, 2019
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.factor.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.util.ArrayList;
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
import org.opensilex.core.factor.dal.FactorDAO;
import org.opensilex.core.factor.dal.FactorLevelDAO;
import org.opensilex.core.factor.dal.FactorLevelModel;
import org.opensilex.core.factor.dal.FactorModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Arnaud Charleroy
 */
@Api(FactorAPI.CREDENTIAL_FACTOR_GROUP_ID)
@Path("/core/factor")
@ApiCredentialGroup(
        groupId = FactorAPI.CREDENTIAL_FACTOR_GROUP_ID,
        groupLabelKey = FactorAPI.CREDENTIAL_FACTOR_GROUP_LABEL_KEY
)
public class FactorAPI {

    public static final String FACTOR_EXAMPLE_URI = "http://opensilex/set/factors/ZA17";

    public static final String CREDENTIAL_FACTOR_GROUP_ID = "Factors";
    public static final String CREDENTIAL_FACTOR_GROUP_LABEL_KEY = "credential-groups.factors";

    public static final String CREDENTIAL_FACTOR_MODIFICATION_ID = "factors-modification";
    public static final String CREDENTIAL_FACTOR_MODIFICATION_LABEL_KEY = "credential.factors.modification";

    public static final String CREDENTIAL_FACTOR_READ_ID = "factors-read";
    public static final String CREDENTIAL_FACTOR_READ_LABEL_KEY = "credential.factors.read";

    public static final String CREDENTIAL_FACTOR_DELETE_ID = "factors-delete";
    public static final String CREDENTIAL_FACTOR_DELETE_LABEL_KEY = "credential.factors.delete";

    public static final String DEFAULT_TRANSLATION_LANGUAGE = "en";

    public static final Logger LOGGER = LoggerFactory.getLogger(FactorAPI.class);

    @Inject
    private SPARQLService sparql;

    @CurrentUser
    UserModel user;

    /**
     * Create a factor model from a FactorCreationDTO object
     *
     * @param dto FactorCreationDTO object
     * @return Factor URI
     * @throws Exception if creation failed
     */
    @POST
    @Path("create")
    @ApiOperation("Create an factor")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_FACTOR_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_FACTOR_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createFactor(
            @ApiParam("Factor description") @Valid FactorCreationDTO dto
    ) throws Exception {
        FactorDAO dao = new FactorDAO(sparql);
        try {
            FactorModel model = dto.newModel();
            dao.create(model);
            List<FactorLevelModel> factorLevelsModels = new ArrayList<>();
            dto.getFactorLevels().forEach(factorCreationDTO -> {
                FactorLevelModel newModel = factorCreationDTO.newModel();
                newModel.setHasFactor(model.getUri());
                factorLevelsModels.add(newModel);
            });
            model.setFactorLevels(factorLevelsModels);
            dao.update(model);
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
    @Path("get/{uri}")
    @ApiOperation("Get an factor")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_FACTOR_READ_ID,
            credentialLabelKey = CREDENTIAL_FACTOR_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Factor retrieved", response = FactorDetailsGetDTO.class),
        @ApiResponse(code = 404, message = "No factor found", response = ErrorResponse.class)
    })
    public Response getFactor(
            @ApiParam(value = "Factor URI", example = "platform-factor:irrigation", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        FactorDAO dao = new FactorDAO(sparql);
        FactorModel model = dao.get(uri);

        if (model != null) {
            return new SingleObjectResponse<>(
                    FactorDetailsGetDTO.fromModel(model)
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
     * Retreive factor  levels by uri
     *
     * @param uri factor uri levels
     * @return Return factor levels
     * @throws Exception in case of server error
     */
    @GET
    @Path("get/{uri}/levels")
    @ApiOperation("Get an factor")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_FACTOR_READ_ID,
            credentialLabelKey = CREDENTIAL_FACTOR_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Factor levels retrieved", response = FactorLevelGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 404, message = "No factor found", response = ErrorResponse.class)
    })
    public Response getFactorLevels(
            @ApiParam(value = "Factor URI", example = "platform-factor:irrigation", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        FactorDAO dao = new FactorDAO(sparql);
        FactorModel model = dao.get(uri);
      
        if (model != null) {
            FactorDetailsGetDTO dtoFromModel = FactorDetailsGetDTO.fromModel(model);
            return new SingleObjectResponse<>(
                    dtoFromModel.getFactorLevels()
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
     * @param factorSearchDTO
     * @see org.opensilex.core.factor.dal.FactorDAO
     * @param orderByList List of fields to sort as an array of
     * fieldName=asc|desc
     * @param page Page number
     * @param pageSize Page size
     * @return filtered, ordered and paginated list
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @POST
    @Path("search")
    @ApiOperation("Search factors")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_FACTOR_READ_ID,
            credentialLabelKey = CREDENTIAL_FACTOR_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return factor list", response = FactorGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class)
    })
    public Response searchFactors(
            @ApiParam("Factor search form") FactorSearchDTO factorSearchDTO,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "name=asc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        // Search factors with Factor DAO
        FactorDAO dao = new FactorDAO(sparql);
        ListWithPagination<FactorModel> resultList = dao.search(
                factorSearchDTO.getName(),
                orderByList,
                page,
                pageSize,
                user.getLanguage()
        );

        List<FactorGetDTO> resultDTOList = new ArrayList<>();

        for (FactorModel factorModel : resultList.getList()) {
            resultDTOList.add(FactorGetDTO.fromModel(factorModel));
        }

        ListWithPagination<FactorGetDTO> paginatedListResponse = new ListWithPagination<FactorGetDTO>(
                resultDTOList, resultList.getPage(),
                resultList.getPageSize(),
                resultList.getTotal()
        );

        // Return paginated list of factor DTO
        return new PaginatedListResponse<>(paginatedListResponse).getResponse();
    }

    /**
     * getAll factors
     *
     * @see org.opensilex.core.factor.dal.FactorDAO
     * @return filtered, ordered and paginated list
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @POST
    @Path("getAll")
    @ApiOperation("Get all factors")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_FACTOR_READ_ID,
            credentialLabelKey = CREDENTIAL_FACTOR_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return factor list", response = FactorGetDTO.class, responseContainer = "List")
    })
    public Response getAllFactors() throws Exception {

        // Search factors with Factor DAO
        FactorDAO dao = new FactorDAO(sparql);
        List<FactorModel> factorModelsList = dao.getAll(user.getLanguage());
        List<FactorGetDTO> getFactorDTOList = new ArrayList<>();
        for (FactorModel factorModel : factorModelsList) {
            FactorGetDTO factorGetDTO = new FactorGetDTO();
            factorGetDTO.setUri(factorModel.getUri());
            factorGetDTO.setName(factorModel.getName().getDefaultLang());
            factorGetDTO.setComment(factorModel.getComment());
            getFactorDTOList.add(factorGetDTO);
        }

        // Return paginated list of factor DTO
        return new PaginatedListResponse<>(getFactorDTOList).getResponse();
    }

    /**
     * Remove an factor
     *
     * @param factorUri the factor URI
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the deleted Factor {@link URI}
     */
    @DELETE
    @Path("delete/{uri}")
    @ApiOperation("Delete an factor")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_FACTOR_DELETE_ID,
            credentialLabelKey = CREDENTIAL_FACTOR_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Factor deleted", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Invalid or unknown Factor URI", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public Response deleteFactor(
            @ApiParam(value = "Factor URI", example = FACTOR_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI factorUri
    ) {
        try {
            FactorDAO dao = new FactorDAO(sparql);
            dao.delete(factorUri);
            return new ObjectUriResponse(factorUri).getResponse();

        } catch (SPARQLInvalidURIException e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "Invalid or unknown Factor URI", e.getMessage()).getResponse();
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }

    /**
     * @param dto the Factor to update
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the updated Factor {@link URI}
     */
    @PUT
    @Path("update")
    @ApiOperation("Update a factor")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_FACTOR_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_FACTOR_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Factor updated", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Invalid or unknown Experiment URI", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public Response updateFactor(
            @ApiParam("Factor description") @Valid FactorUpdateDTO dto
    ) {
        try {
            FactorDAO factorDao = new FactorDAO(sparql);
            FactorLevelDAO factorLevelDao = new FactorLevelDAO(sparql);
            FactorModel model = dto.newModel();
            List<FactorLevelModel> existingFactors = factorLevelDao.search(model.getUri());
            existingFactors.forEach(factorLevelModel -> {
                System.out.println("org.opensilex.core.factor.api.FactorAPI.updateFactor()");
                System.out.println(factorLevelModel.getUri().toString());
                try {
                    factorLevelDao.delete(factorLevelModel.getUri());
                } catch (Exception ex) {
                    System.out.println("org.opensilex.core.factor.api.FactorAPI.updateFactor() ERROR" + ex.getMessage());
                 }
            });

            List<FactorLevelModel> factorLevelsModels = new ArrayList<>();
            dto.getFactorLevels().forEach(factorCreationDTO -> {
                FactorLevelModel newModel = factorCreationDTO.newModel();
                newModel.setHasFactor(model.getUri());
                factorLevelsModels.add(newModel);

            });
            factorLevelDao.create(factorLevelsModels);
            model.setFactorLevels(factorLevelsModels); 
            factorDao.update(model);
            
            return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();

        } catch (SPARQLInvalidURIException e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "Invalid or unknown Factor URI", e.getMessage()).getResponse();
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }
}
