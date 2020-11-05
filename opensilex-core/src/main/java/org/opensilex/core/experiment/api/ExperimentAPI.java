//******************************************************************************
//                          ExperimentAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.experiment.api;

import io.swagger.annotations.*;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.Max;
import javax.validation.constraints.Size;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.factor.api.FactorDetailsGetDTO;
import org.opensilex.core.factor.dal.FactorDAO;
import org.opensilex.core.factor.dal.FactorModel;
import org.opensilex.core.infrastructure.api.InfrastructureFacilityGetDTO;
import org.opensilex.core.infrastructure.dal.InfrastructureFacilityModel;
import org.opensilex.core.species.api.SpeciesDTO;
import org.opensilex.core.species.dal.SpeciesDAO;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.datanucleus.DataNucleusService;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.response.NamedResourceDTO;

/**
 * @author Vincent MIGOT
 * @author Renaud COLIN
 */
@Api(ExperimentAPI.CREDENTIAL_EXPERIMENT_GROUP_ID)
@Path("/core/experiment")
@ApiCredentialGroup(
        groupId = ExperimentAPI.CREDENTIAL_EXPERIMENT_GROUP_ID,
        groupLabelKey = ExperimentAPI.CREDENTIAL_EXPERIMENT_GROUP_LABEL_KEY
)
public class ExperimentAPI {

    public static final String CREDENTIAL_EXPERIMENT_GROUP_ID = "Experiments";
    public static final String CREDENTIAL_EXPERIMENT_GROUP_LABEL_KEY = "credential-groups.experiments";

    public static final String CREDENTIAL_EXPERIMENT_MODIFICATION_ID = "experiment-modification";
    public static final String CREDENTIAL_EXPERIMENT_MODIFICATION_LABEL_KEY = "credential.experiment.modification";

    public static final String CREDENTIAL_EXPERIMENT_DELETE_ID = "experiment-delete";
    public static final String CREDENTIAL_EXPERIMENT_DELETE_LABEL_KEY = "credential.experiment.delete";

    public static final String EXPERIMENT_EXAMPLE_URI = "http://opensilex/set/experiments/ZA17";

    @CurrentUser
    UserModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private DataNucleusService nosql;
    
    @Inject
    private FileStorageService fs;

    /**
     * Create an Experiment
     *
     * @param dto the Experiment to create
     * @return a {@link Response} with a {@link ObjectUriResponse} containing the created Experiment {@link URI}
     * @throws java.lang.Exception
     */
    @POST
    @Path("create")
    @ApiOperation("Create an experiment")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_EXPERIMENT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_EXPERIMENT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Create an experiment", response = ObjectUriResponse.class),
        @ApiResponse(code = 409, message = "An experiment with the same URI already exists", response = ErrorResponse.class)
    })
    public Response createExperiment(
            @ApiParam("Experiment description") @Valid ExperimentCreationDTO dto
    ) throws Exception {
        try {
            ExperimentDAO dao = new ExperimentDAO(sparql);
            ExperimentModel model = dto.newModel();
            model.setCreator(currentUser.getUri());

            model = dao.create(model);
            return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();
        } catch (SPARQLAlreadyExistingUriException e) {
            // Return error response 409 - CONFLICT if experiment URI already exists
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Experiment already exists",
                    "Duplicated URI: " + e.getUri()
            ).getResponse();
        }
    }

    /**
     * @param xpDto the Experiment to update
     * @return a {@link Response} with a {@link ObjectUriResponse} containing the updated Experiment {@link URI}
     */
    @PUT
    @Path("update")
    @ApiOperation("Update an experiment")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_EXPERIMENT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_EXPERIMENT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Experiment updated", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Experiment URI not found", response = ErrorResponse.class)
    })
    public Response updateExperiment(
            @ApiParam("Experiment description") @Valid ExperimentCreationDTO xpDto
    ) throws Exception {
        ExperimentDAO dao = new ExperimentDAO(sparql);

        ExperimentModel model = xpDto.newModel();
        dao.update(model, currentUser);
        return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();
    }

    /**
     * @param xpUri the Experiment URI
     * @return a {@link Response} with a {@link SingleObjectResponse} containing the {@link ExperimentGetDTO}
     */
    @GET
    @Path("get/{uri}")
    @ApiOperation("Get an experiment by URI")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Experiment retrieved", response = ExperimentGetDTO.class),
        @ApiResponse(code = 404, message = "Experiment URI not found", response = ErrorResponse.class)
    })
    public Response getExperiment(
            @ApiParam(value = "Experiment URI", example = "http://opensilex.dev/set/experiments/ZA17", required = true) @PathParam("uri") @NotNull URI xpUri
    ) throws Exception {
        ExperimentDAO dao = new ExperimentDAO(sparql);
        ExperimentModel model = dao.get(xpUri, currentUser);
        return new SingleObjectResponse<>(ExperimentGetDTO.fromModel(model)).getResponse();
    }

    /**
     * Search experiments
     *
     * @param year
     * @param label
     * @param species
     * @param projects
     * @param isPublic
     * @param isEnded
     * @param orderByList
     * @param page
     * @param pageSize
     * @return filtered, ordered and paginated list
     * @throws java.lang.Exception
     * @see ExperimentDAO
     */
    @GET
    @Path("search")
    @ApiOperation("Search Experiments")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return Experiment list", response = ExperimentGetListDTO.class, responseContainer = "List")
    })
    public Response searchExperiments(
            @ApiParam(value = "Search by year", example = "2017") @QueryParam("year")  @Min(999) @Max(10000) Integer year,
            @ApiParam(value = "Regex pattern for filtering by label", example = "ZA17") @QueryParam("label") String label,
            @ApiParam(value = "Search by involved species", example = "http://www.phenome-fppn.fr/id/species/zeamays") @QueryParam("species") List<URI> species,
            @ApiParam(value = "Search by studied effect", example = "http://purl.obolibrary.org/obo/CHEBI_25555") @QueryParam("factors") List<URI> factors,
            @ApiParam(value = "Search by related project uri", example = "http://www.phenome-fppn.fr/projects/ZA17\nhttp://www.phenome-fppn.fr/id/projects/ZA18") @QueryParam("projects") List<URI> projects,
            @ApiParam(value = "Search private(false) or public experiments(true)") @QueryParam("isPublic") Boolean isPublic,
            @ApiParam(value = "Search ended(false) or active experiments(true)") @QueryParam("isEnded") Boolean isEnded,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "label=asc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        ExperimentDAO xpDao = new ExperimentDAO(sparql);

        ListWithPagination<ExperimentModel> resultList = xpDao.search(
                year,
                label,
                species,
                factors,
                isEnded,
                projects,
                isPublic,
                currentUser,
                orderByList,
                page,
                pageSize
        );

        // Convert paginated list to DTO
        ListWithPagination<ExperimentGetListDTO> resultDTOList = resultList.convert(ExperimentGetListDTO.class, ExperimentGetListDTO::fromModel);
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    /**
     * Remove an experiment
     *
     * @param xpUri the experiment URI
     * @return a {@link Response} with a {@link ObjectUriResponse} containing the deleted Experiment {@link URI}
     */
    @DELETE
    @Path("delete/{uri}")
    @ApiOperation("Delete an experiment")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_EXPERIMENT_DELETE_ID,
            credentialLabelKey = CREDENTIAL_EXPERIMENT_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Experiment deleted", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Experiment URI not found", response = ErrorResponse.class)
    })
    public Response deleteExperiment(
            @ApiParam(value = "Experiment URI", example = EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI xpUri
    ) throws Exception {
        ExperimentDAO dao = new ExperimentDAO(sparql);
        dao.delete(xpUri, currentUser);
        return new ObjectUriResponse(xpUri).getResponse();
    }

    @PUT
    @Path("{uri}/set-facilities")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_EXPERIMENT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_EXPERIMENT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return experiment URI", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Experiment URI not found", response = ErrorResponse.class)
    })
    public Response setFacilities(
            @ApiParam(value = "Experiment URI", example = EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI xpUri,
            @ApiParam(value = "Facility URI") List<URI> facitilities) throws Exception {
        ExperimentDAO xpDao = new ExperimentDAO(sparql);
        xpDao.setFacilities(xpUri, facitilities, currentUser);
        return new ObjectUriResponse(Response.Status.OK, xpUri).getResponse();
    }

    @GET
    @Path("{uri}/get-facilities")
    @ApiOperation("get all facilities involved in an experiment")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return facilities list", response = InfrastructureFacilityGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })
    public Response getFacilities(
            @ApiParam(value = "Experiment URI", example = EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI xpUri
    ) throws Exception {
        ExperimentDAO xpDao = new ExperimentDAO(sparql);
        List<InfrastructureFacilityModel> facilities = xpDao.getFacilities(xpUri, currentUser);

        List<InfrastructureFacilityGetDTO> dtoList = facilities.stream().map(InfrastructureFacilityGetDTO::getDTOFromModel).collect(Collectors.toList());
        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @GET
    @Path("{uri}/get-available-facilities")
    @ApiOperation("get all facilities available for an experiment (depending of which infrastructures are selected)")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return facilities list", response = InfrastructureFacilityGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })
    public Response getAvailableFacilities(
            @ApiParam(value = "Experiment URI", example = EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI xpUri
    ) throws Exception {
        ExperimentDAO xpDao = new ExperimentDAO(sparql);
        List<InfrastructureFacilityModel> facilities = xpDao.getAvailableFacilities(xpUri, currentUser);

        List<InfrastructureFacilityGetDTO> dtoList = facilities.stream().map(InfrastructureFacilityGetDTO::getDTOFromModel).collect(Collectors.toList());
        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @GET
    @Path("get-available-species/{uri}")
    @ApiOperation("get all species associated to an experiment")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return Species list", response = SpeciesDTO.class, responseContainer = "List")
    })
    public Response getAvailableSpecies(
            @ApiParam(value = "Experiment URI", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI xpUri
    ) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(xpUri, currentUser);

        SpeciesDAO dao = new SpeciesDAO(sparql);
        List<SpeciesModel> species = dao.getByExperiment(xpUri, currentUser.getLanguage());

        List<SpeciesDTO> dtoList = species.stream().map(SpeciesDTO::fromModel).collect(Collectors.toList());
        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @GET
    @Path("get-available-factors/{uri}")
    @ApiOperation("get all factors with their levels associated to an experiment")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return factors list", response = FactorDetailsGetDTO.class, responseContainer = "List")
    })
    public Response getAvailableFactors(
            @ApiParam(value = "Experiment URI", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI xpUri
    ) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(xpUri, currentUser);

        FactorDAO dao = new FactorDAO(sparql);
        List<FactorModel> factors = dao.getByExperiment(xpUri, currentUser.getLanguage());

        List<FactorDetailsGetDTO> dtoList = factors.stream().map(FactorDetailsGetDTO::fromModel).collect(Collectors.toList());
        return new PaginatedListResponse<>(dtoList).getResponse();
    }

//    @GET
//    @Path("get-used-variables/{uri}")
//    @ApiOperation("get all variables related to an experiment")
//    @ApiProtected
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//    @ApiResponses(value = {
//        @ApiResponse(code = 200, message = "Return variables list", response = NamedResourceDTO.class, responseContainer = "List")
//    })
//    public Response getUsedVariables(
//            @ApiParam(value = "Experiment URI", example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI xpUri
//    ) throws Exception {
//        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
//        xpDAO.validateExperimentAccess(xpUri, currentUser);
//
//        DataDAOBack dao = new DataDAOBack(nosql, sparql, fs);
//        List<VariableModel> variables = dao.getVariablesByExperiment(xpUri, currentUser.getLanguage());
//
//        List<NamedResourceDTO> dtoList = variables.stream().map(NamedResourceDTO::getDTOFromModel).collect(Collectors.toList());
//        return new PaginatedListResponse<>(dtoList).getResponse();
//
//    }

    /**
     * Updates the factors linked to an experiment.
     *
     * @param xpUri
     * @param factors
     * @return the query result
     * @example [ "http://www.phenome-fppn.fr/opensilex/2018/s18001" ]
     * @example { "metadata": { "pagination": null, "status": [ { "message": "Resources updated", "exception": { "type":
     * "Info", "href": null, "details": "The experiment http://www.opensilex.fr/platform/OSL2018-1 has now 1 linked
     * sensors" } } ], "datafiles": [ "http://www.opensilex.fr/platform/OSL2015-1" ] } }
     */
    @PUT
    @Path("{uri}/factors")
    @ApiOperation("Update the factors which participates in an experiment")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_EXPERIMENT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_EXPERIMENT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "The list of factors which participates in the experiment updated", response = ObjectUriResponse.class),
        @ApiResponse(code = 404, message = "Experiment URI not found", response = ErrorResponse.class)
    })
    public Response putFactors(
            @ApiParam(value = "Experiment URI", example = EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI xpUri,
            @ApiParam(value = "List of factors uris") ArrayList<URI> factors) throws Exception {
        ExperimentDAO xpDao = new ExperimentDAO(sparql);
        xpDao.updateWithFactors(xpUri, factors, currentUser);
        return new ObjectUriResponse(Response.Status.OK, xpUri).getResponse();
    }
}
