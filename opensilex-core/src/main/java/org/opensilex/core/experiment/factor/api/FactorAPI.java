/*
 * ******************************************************************************
 *                                     FactorAPI.java
 *  OpenSILEX
 *  Copyright © INRA 2019
 *  Creation date:  17 December, 2019
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.experiment.factor.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import org.opensilex.OpenSilex;
import org.opensilex.core.experiment.api.ExperimentGetListDTO;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorDAO;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.*;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.response.*;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.model.SPARQLTreeListModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.store.OntologyStore;
import org.opensilex.sparql.response.CreatedUriResponse;
import org.opensilex.sparql.response.ResourceTreeDTO;
import org.opensilex.sparql.response.ResourceTreeResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author Arnaud Charleroy
 */
@Path("/core/experiments/factors")
@Tag(name = FactorAPI.CREDENTIAL_FACTOR_GROUP_ID)
@ApiCredentialGroup(
        groupId = FactorAPI.CREDENTIAL_FACTOR_GROUP_ID,
        groupLabelKey = FactorAPI.CREDENTIAL_FACTOR_GROUP_LABEL_KEY
)
public class FactorAPI {

    public static final String FACTOR_EXAMPLE_URI = "platform-factor:irrigation";

    public static final String CREDENTIAL_FACTOR_GROUP_ID = "Factors";
    public static final String CREDENTIAL_FACTOR_GROUP_LABEL_KEY = "credential-groups.factors";

    public static final String CREDENTIAL_FACTOR_MODIFICATION_ID = "factor-modification";
    public static final String CREDENTIAL_FACTOR_MODIFICATION_LABEL_KEY = "credential.default.modification";

    public static final String CREDENTIAL_FACTOR_DELETE_ID = "factor-delete";
    public static final String CREDENTIAL_FACTOR_DELETE_LABEL_KEY = "credential.default.delete";

    public static final Logger LOGGER = LoggerFactory.getLogger(FactorAPI.class);

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @CurrentUser
    AccountModel currentUser;

    /**
     * Create a factor model from a FactorCreationDTO object
     *
     * @param dto FactorCreationDTO object
     * @return Factor URI
     * @throws Exception if creation failed
     */
    @POST
    @Operation(summary = "Create a factor")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_FACTOR_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_FACTOR_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createFactor(
            @Parameter(description = "Factor description") @Valid FactorCreationDTO dto) throws Exception {
        ExperimentDAO experimentDAO = new ExperimentDAO(sparql, nosql);
        ExperimentModel xpModel = experimentDAO.get(dto.getExperiment(), currentUser);
        FactorDAO dao = new FactorDAO(sparql);
        try {
            FactorModel model = dto.newModel();
            model.setPublisher(currentUser.getUri());
            List<FactorLevelModel> factorLevelsModels = new ArrayList<>();
            for (FactorLevelCreationDTO factorCreationDTO : dto.getFactorLevels()) {
                FactorLevelModel newModel = factorCreationDTO.newModel();
                newModel.setPublisher(currentUser.getUri());
                newModel.setFactor(model);
                factorLevelsModels.add(newModel);
            }
            model.setFactorLevels(factorLevelsModels);
            List<ExperimentModel> associatedExperiment = new ArrayList<>();
            associatedExperiment.add(xpModel);
            model.setExperiment(xpModel);
            model.setAssociatedExperiments(associatedExperiment);
            dao.create(model);

            return new CreatedUriResponse(model.getUri()).getResponse();
        } catch (SPARQLAlreadyExistingUriException duplicateUriException) {
            return new ErrorResponse(Response.Status.CONFLICT, "Factor already exists",
                    duplicateUriException.getMessage()).getResponse();
        }
    }

    /**
     * Retrieve factor by uri
     *
     * @param uri factor uri
     * @return Return factor detail
     * @throws Exception in case of server error
     */
    @GET
    @Path("{uri}")
    @Operation(summary = "Get a factor")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Factor retrieved", content = @Content(schema = @Schema(implementation = FactorDetailsGetDTO.class))),
        @ApiResponse(responseCode = "404", description = "Factor not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    public Response getFactorByURI(
            @Parameter(description = "Factor URI", example = FACTOR_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI uri)
            throws Exception {

        FactorDAO dao = new FactorDAO(sparql);
        FactorModel model = dao.get(uri);

        if (model != null) {
            ExperimentDAO experimentDAO = new ExperimentDAO(sparql, nosql);
            experimentDAO.validateExperimentAccess(model.getExperiment().getUri(), currentUser);
            FactorDetailsGetDTO dto = FactorDetailsGetDTO.fromModel(model);
            if (Objects.nonNull(model.getPublisher())){
                dto.setPublisher(UserGetDTO.fromModel(new AccountDAO(sparql).get(model.getPublisher())));
            }
            return new SingleObjectResponse<>(dto).getResponse();
        } else {
            throw new NotFoundURIException("Factor URI not found: ", uri);
        }
    }

    /**
     * Retrieve factor levels by factor uri
     *
     * @param uri factor uri levels
     * @return Return factor levels
     * @throws Exception in case of server error
     */
    @GET
    @Path("{uri}/levels")
    @Operation(summary = "Get factor levels")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Factor retrieved", content = @Content(array = @ArraySchema(schema = @Schema(implementation = FactorLevelGetDTO.class)))),
        @ApiResponse(responseCode = "404", description = "Factor not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    public Response getFactorLevels(
            @Parameter(description = "Factor URI", example = FACTOR_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI uri)
            throws Exception {
        FactorDAO dao = new FactorDAO(sparql);
        FactorModel model = dao.get(uri);

        if (model != null) {
            ExperimentDAO experimentDAO = new ExperimentDAO(sparql, nosql);
            experimentDAO.validateExperimentAccess(model.getExperiment().getUri(), currentUser);

            FactorDetailsGetDTO dtoFromModel = FactorDetailsGetDTO.fromModel(model);
            return new SingleObjectResponse<>(dtoFromModel.getFactorLevels()).getResponse();
        } else {
            throw new NotFoundURIException("Factor URI not found: ", uri);
        }
    }

    /**
     * Retrieve experiments which study effects of this factor
     *
     * @param uri factor uri
     * @return Return factor associated experiments
     * @throws Exception in case of server error
     */
    @GET
    @Path("{uri}/experiments")
    @Operation(summary = "Get factor associated experiments")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Experiments retrieved", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ExperimentGetListDTO.class)))),
        @ApiResponse(responseCode = "404", description = "Factor not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    public Response getFactorAssociatedExperiments(
            @Parameter(description = "Factor URI", example = FACTOR_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI uri)
            throws Exception {
        FactorDAO dao = new FactorDAO(sparql);
        FactorModel model = dao.get(uri);
        if (model != null) {
            ExperimentDAO experimentDAO = new ExperimentDAO(sparql, nosql);
            experimentDAO.validateExperimentAccess(model.getExperiment().getUri(), currentUser);

            List<ExperimentModel> experiments = model.getAssociatedExperiments();
            // TODO: filter experiments with those accessible to current user
            ListWithPagination<ExperimentModel> experimentsList = new ListWithPagination(experiments);
            // Convert paginated list to DTO
            ListWithPagination<ExperimentGetListDTO> convertList = experimentsList.convert(ExperimentGetListDTO.class, ExperimentGetListDTO::fromModel);
            return new PaginatedListResponse<>(convertList).getResponse();
        } else {
            throw new NotFoundURIException("Factor URI not found: ", uri);
        }
    }

    /**
     * Search factors
     *
     * @param name
     * @param comment
     * @param category
     * @param experiment
     * @see org.opensilex.core.experiment.factor.dal.FactorDAO
     * @param orderByList List of fields to sort as an array of fieldName=asc|desc
     * @param page Page number
     * @param pageSize Page size
     * @return filtered, ordered and paginated list
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @GET
    @Operation(summary = "Search factors")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Return factor list", content = @Content(array = @ArraySchema(schema = @Schema(implementation = FactorGetDTO.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "404", description = "Experiment URI not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    public Response searchFactors(
            @Parameter(description = "Regex pattern for filtering on name", example = "irrigation") @QueryParam("name") String name,
            @Parameter(description = "Regex pattern for filtering on description", example = "20ml of water") @QueryParam("description") String comment,
            @Parameter(description = "Filter by category of a factor", example = "http://aims.fao.org/aos/agrovoc/c_32668") @QueryParam("category") URI category,
            @Parameter(description = "Filter by experiment", example = "demo-exp:experiment1") @QueryParam("experiment") URI experiment,
            @Parameter(description = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @Parameter(description = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @Parameter(description = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize)
            throws Exception {

        // Search factors with Factor DAO
        FactorDAO dao = new FactorDAO(sparql);
        ExperimentDAO experimentDAO = new ExperimentDAO(sparql, nosql);
        try {
            List<URI> experiments = null;
            if (experiment != null) {
                experimentDAO.validateExperimentAccess(experiment, currentUser);
                experiments = new ArrayList<>();
                experiments.add(experiment);
            } else {
                Set<URI> userExperiments = experimentDAO.getUserExperiments(currentUser);
                experiments = new ArrayList<>(userExperiments);
            }

            ListWithPagination<FactorModel> resultList = dao.search(name, null, category, experiments, orderByList, page, pageSize,
                    currentUser.getLanguage(), false);

            List<FactorGetDTO> resultDTOList = new ArrayList<>();

            for (FactorModel factorModel : resultList.getList()) {
                resultDTOList.add(FactorGetDTO.fromModel(factorModel));
            }

            ListWithPagination<FactorGetDTO> paginatedListResponse = new ListWithPagination<>(resultDTOList,
                    resultList.getPage(), resultList.getPageSize(), resultList.getTotal());

            // Return paginated list of factor DTO
            return new PaginatedListResponse<>(paginatedListResponse).getResponse();
            // No access to uri
        } catch (ForbiddenURIAccessException e) {
            List<FactorGetDTO> resultDTOList = new ArrayList<>();
            ListWithPagination<FactorGetDTO> paginatedListResponse = new ListWithPagination<>(resultDTOList);
            return new PaginatedListResponse<>(paginatedListResponse).getResponse();
        }
    }

    @GET
    @Path("factor_levels")
    @Operation(summary = "Search factors levels")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Return factor list with their levels", content = @Content(array = @ArraySchema(schema = @Schema(implementation = FactorDetailsGetDTO.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))})
    public Response searchFactorLevels(
            @Parameter(description = "Regex pattern for filtering on name", example = "WW") @QueryParam("name") String name,
            @Parameter(description = "List of fields to sort as an array of fieldName=asc|desc", example = "name=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @Parameter(description = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @Parameter(description = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize)
            throws Exception {

        FactorDAO dao = new FactorDAO(sparql);
        ListWithPagination<FactorModel> factors = dao.search(null, name, null, null, orderByList, page, pageSize, currentUser.getLanguage(), true);
        ListWithPagination<FactorDetailsGetDTO> dtoList = factors.convert(FactorDetailsGetDTO.class, FactorDetailsGetDTO::fromModel);

        // Return paginated list of factor DTO
        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    /**
     * Remove an factor
     *
     * @param uri the factor URI
     * @return a {@link Response} with a {@link ObjectUriResponse} containing the deleted Factor {@link URI}
     */
    @DELETE
    @Path("{uri}")
    @Operation(summary = "Delete a factor")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_FACTOR_DELETE_ID,
            credentialLabelKey = CREDENTIAL_FACTOR_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Factor deleted", content = @Content(schema = @Schema(implementation = URI.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or unknown Factor URI", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    public Response deleteFactor(
            @Parameter(description = "Factor URI", example = FACTOR_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI uri) throws Exception {
        try {
            FactorDAO dao = new FactorDAO(sparql);
            FactorModel model = dao.get(uri);
            if (model != null) {
                ExperimentDAO experimentDAO = new ExperimentDAO(sparql, nosql);
                experimentDAO.validateExperimentAccess(model.getExperiment().getUri(), currentUser);
                // Get associated experiment URIs
                List<ExperimentModel> associatedFactorExperimentURIs = model.getAssociatedExperiments();

                if (associatedFactorExperimentURIs.size() > 1) {
                    return new ErrorResponse(
                            Response.Status.BAD_REQUEST,
                            "The factor is linked to multiple experiment(s)",
                            "You can't delete a factor linked to another experiment"
                    ).getResponse();
                }

                dao.delete(uri);
                return new ObjectUriResponse(uri).getResponse();
            } else {
                throw new NotFoundURIException("Factor URI not found: ", uri);
            }
        } catch (SPARQLInvalidURIException e) {
            throw new NotFoundURIException("Factor URI not found: ", uri);
        }
    }

    /**
     * @param dto the Factor to update
     * @return a {@link Response} with a {@link ObjectUriResponse} containing the updated Factor {@link URI}
     */
    @PUT
    @Operation(summary = "Update a factor")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_FACTOR_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_FACTOR_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Factor updated", content = @Content(schema = @Schema(implementation = URI.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or unknown Experiment URI", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    public Response updateFactor(@Parameter(description = "Factor description") @Valid FactorUpdateDTO dto) {

        try {
            // Update factor
            FactorDAO factorDao = new FactorDAO(sparql);
            FactorModel existingModel = factorDao.get(dto.getUri());

            ExperimentDAO experimentDAO = new ExperimentDAO(sparql, nosql);
            experimentDAO.validateExperimentAccess(existingModel.getExperiment().getUri(), currentUser);
            FactorModel model = dto.newModel();
            List<FactorLevelModel> factorLevelsModels = new ArrayList<>();
            for (FactorLevelCreationDTO factorCreationDTO : dto.getFactorLevels()) {
                FactorLevelModel newModel = factorCreationDTO.newModel();
                newModel.setFactor(model);
                factorLevelsModels.add(newModel);
            }
            model.setFactorLevels(factorLevelsModels);
            model.setExperiment(existingModel.getExperiment());
            model.setAssociatedExperiments(existingModel.getAssociatedExperiments());
            factorDao.update(model);

            return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();

        } catch (SPARQLInvalidURIException e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "Invalid or unknown Factor URI", e.getMessage())
                    .getResponse();
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }

    /**
     * * Return a list of factors corresponding to the given URIs
     *
     * @param uris list of factors uri
     * @return Corresponding list of factors
     * @throws Exception Return a 500 - INTERNAL_SERVER_ERROR error response
     */
    @GET
    @Path("by_uris")
    @Operation(summary = "Get a list of factors by their URIs")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Return factors list", content = @Content(array = @ArraySchema(schema = @Schema(implementation = FactorGetDTO.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid parameters", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "404", description = "Factor not found (if any provided URIs is not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public Response getFactorsByURIs(
            @Parameter(description = "Factors URIs", required = true) @QueryParam("uris") @NotNull List<URI> uris
    ) throws Exception {
        FactorDAO dao = new FactorDAO(sparql);
        List<FactorModel> models = dao.getList(uris);

        ExperimentDAO experimentDAO = new ExperimentDAO(sparql, nosql);
        Set<URI> userExperiments = experimentDAO.getUserExperiments(currentUser);
        if (!models.isEmpty()) {
            List<FactorGetDTO> resultDTOList = new ArrayList<>(models.size());
            models.forEach(result -> {
                if (userExperiments.contains(result.getExperiment().getUri())) {
                    resultDTOList.add(FactorGetDTO.fromModel(result));
                }
            });

            return new PaginatedListResponse<>(resultDTOList).getResponse();
        } else {
            // Otherwise return a 404 - NOT_FOUND error response
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Factors not found",
                    "Unknown factor URIs"
            ).getResponse();
        }
    }

    @GET
    @Path("/categories")
    @Operation(summary = "Search categories")
    @Parameters({
        @Parameter(name = HttpHeaders.ACCEPT_LANGUAGE,
                schema = @Schema(type = "string"),
                in = ParameterIn.HEADER,
                description = "Request accepted language",
                example = OpenSilex.DEFAULT_LANGUAGE)
    })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Return categories", content = @Content(array = @ArraySchema(schema = @Schema(implementation = FactorCategoryGetDTO.class))))
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchCategories(
            @Parameter(description = "Category name regex pattern", example = "describing") @QueryParam("name") String namePattern,
            @Parameter(description = "List of fields to sort as an array of fieldName=asc|desc", example = "name=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList
    ) throws Exception {

        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();
        SPARQLTreeListModel<ClassModel> treeList = ontologyStore.searchSubClasses(new URI(Oeso.FactorCategory.getURI()), namePattern, currentUser.getLanguage(), true);

        List<ResourceTreeDTO> treeDto = ResourceTreeDTO.fromResourceTree(treeList);
        return new ResourceTreeResponse(treeDto).getResponse();
    }

    @GET
    @Path("count")
    @Operation(summary = "Count factors")
    @ApiProtected
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Return the number of factors associated to a given experiment", content = @Content(schema = @Schema(implementation = Integer.class)))
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response countFactors(
            @Parameter(description = "Experiment URI", example = "http://www.opensilex.org/demo/2018/o18000076") @QueryParam("experiment") URI experiment) throws Exception {

        FactorDAO dao = new FactorDAO(sparql);
        int factorCount = dao.countFactors(experiment);

        return new SingleObjectResponse<>(factorCount).getResponse();
    }
}
