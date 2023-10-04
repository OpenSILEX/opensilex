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
import org.opensilex.core.experiment.dal.ExperimentSearchFilter;
import org.opensilex.core.experiment.factor.api.FactorDetailsGetDTO;
import org.opensilex.core.experiment.factor.dal.FactorDAO;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.organisation.api.facility.FacilityGetDTO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.species.api.SpeciesDTO;
import org.opensilex.core.species.dal.SpeciesDAO;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.*;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.response.CreatedUriResponse;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Vincent MIGOT
 * @author Renaud COLIN
 * @author Julien BONNEFONT
 */
@Api(ExperimentAPI.CREDENTIAL_EXPERIMENT_GROUP_ID)
@Path(ExperimentAPI.PATH)
@ApiCredentialGroup(
        groupId = ExperimentAPI.CREDENTIAL_EXPERIMENT_GROUP_ID,
        groupLabelKey = ExperimentAPI.CREDENTIAL_EXPERIMENT_GROUP_LABEL_KEY
)
public class ExperimentAPI {

    Logger LOGGER = LoggerFactory.getLogger(ExperimentAPI.class);

    public static final String PATH = "/core/experiments";
    public static final String CREDENTIAL_EXPERIMENT_GROUP_ID = "Experiments";
    public static final String CREDENTIAL_EXPERIMENT_GROUP_LABEL_KEY = "credential-groups.experiments";

    public static final String CREDENTIAL_EXPERIMENT_MODIFICATION_ID = "experiment-modification";
    public static final String CREDENTIAL_EXPERIMENT_MODIFICATION_LABEL_KEY = "credential.default.modification";

    public static final String CREDENTIAL_EXPERIMENT_DELETE_ID = "experiment-delete";
    public static final String CREDENTIAL_EXPERIMENT_DELETE_LABEL_KEY = "credential.default.delete";

    public static final String EXPERIMENT_EXAMPLE_URI = "http://opensilex/experiment/id/ZA17";
    public static final String EXPERIMENT_API_VALUE = "Experiment URI";


    public static final int CSV_NB_ERRORS_MAX = 100;

    @CurrentUser
    AccountModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    @Inject
    private FileStorageService fs;

    /**
     * Create an Experiment
     *
     * @param dto the Experiment to create
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the created Experiment {@link URI}
     * @throws java.lang.Exception
     */
    @POST
    @ApiOperation("Add an experiment")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_EXPERIMENT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_EXPERIMENT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "An experiment is created", response = URI.class),
        @ApiResponse(code = 409, message = "An experiment with the same URI already exists", response = ErrorResponse.class)
    })
    public Response createExperiment(
            @ApiParam("Experiment description") @Valid ExperimentCreationDTO dto
    ) throws Exception {
        try {
            ExperimentDAO dao = new ExperimentDAO(sparql, nosql);
            ExperimentModel model = dto.newModel();
            model.setCreator(currentUser.getUri());

            model = dao.create(model);
            return new CreatedUriResponse(model.getUri()).getResponse();
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
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the updated Experiment {@link URI}
     */
    @PUT
    @ApiOperation("Update an experiment")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_EXPERIMENT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_EXPERIMENT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Experiment updated", response = URI.class),
        @ApiResponse(code = 404, message = "Experiment URI not found", response = ErrorResponse.class)
    })
    public Response updateExperiment(
            @ApiParam("Experiment description") @Valid ExperimentCreationDTO xpDto
    ) throws Exception {
        ExperimentDAO dao = new ExperimentDAO(sparql, nosql);

        ExperimentModel model = xpDto.newModel();
        dao.update(model, currentUser);
        return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();
    }

    /**
     * @param xpUri the Experiment URI
     * @return a {@link Response} with a {@link SingleObjectResponse} containing
     * the {@link ExperimentGetDTO}
     */
    @GET
    @Path("{uri}")
    @ApiOperation("Get an experiment")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Experiment retrieved", response = ExperimentGetDTO.class),
        @ApiResponse(code = 404, message = "Experiment URI not found", response = ErrorResponse.class)
    })
    public Response getExperiment(
            @ApiParam(value = EXPERIMENT_API_VALUE, example = EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI xpUri
    ) throws Exception {
        ExperimentDAO dao = new ExperimentDAO(sparql, nosql);
        ExperimentModel model = dao.get(xpUri, currentUser);
        return new SingleObjectResponse<>(ExperimentGetDTO.fromModel(model)).getResponse();
    }

    /**
     * Search experiments
     *
     * @param name
     * @param year
     * @param isEnded
     * @param species
     * @param factorCategories
     * @param projects
     * @param isPublic
     * @param facilities
     * @param orderByList
     * @param page
     * @param pageSize
     * @return filtered, ordered and paginated list
     * @throws java.lang.Exception
     * @see ExperimentDAO
     */
    @GET
    @ApiOperation("Search experiments")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return experiments", response = ExperimentGetListDTO.class, responseContainer = "List")
    })
    public Response searchExperiments(
            @ApiParam(value = "Regex pattern for filtering by name", example = "ZA17") @QueryParam("name") String name,
            @ApiParam(value = "Search by year", example = "2017") @QueryParam("year") Integer year,
            @ApiParam(value = "Search ended(false) or active experiments(true)") @QueryParam("is_ended") Boolean isEnded,
            @ApiParam(value = "Search by involved species", example = "http://www.phenome-fppn.fr/id/species/zeamays") @QueryParam("species") List<URI> species,
            @ApiParam(value = "Search by studied effect", example = "http://purl.obolibrary.org/obo/CHEBI_25555") @QueryParam("factors") List<URI> factorCategories,
            @ApiParam(value = "Search by related project uri", example = "http://www.phenome-fppn.fr/projects/ZA17\nhttp://www.phenome-fppn.fr/id/projects/ZA18") @QueryParam("projects") List<URI> projects,
            @ApiParam(value = "Search private(false) or public experiments(true)") @QueryParam("is_public") Boolean isPublic,
            @ApiParam(value = "Search by involved facilities") @QueryParam("facilities") List<URI> facilities,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        ExperimentDAO xpDao = new ExperimentDAO(sparql, nosql);

        ExperimentSearchFilter filter = new ExperimentSearchFilter()
                .setYear(year)
                .setName(name)
                .setSpecies(species)
                .setFactorCategories(factorCategories)
                .setEnded(isEnded)
                .setProjects(projects)
                .setPublic(isPublic)
                .setFacilities(facilities)
                .setUser(currentUser);

        filter.setOrderByList(orderByList)
                .setPage(page)
                .setPageSize(pageSize);

        ListWithPagination<ExperimentModel> resultList = xpDao.search(filter);

        // Convert paginated list to DTO
        ListWithPagination<ExperimentGetListDTO> resultDTOList = resultList.convert(ExperimentGetListDTO.class, ExperimentGetListDTO::fromModel);
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    /**
     * Remove an experiment
     *
     * @param xpUri the experiment URI
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the deleted Experiment {@link URI}
     */
    @DELETE
    @Path("{uri}")
    @ApiOperation("Delete an experiment")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_EXPERIMENT_DELETE_ID,
            credentialLabelKey = CREDENTIAL_EXPERIMENT_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Experiment deleted", response = URI.class),
        @ApiResponse(code = 404, message = "Experiment URI not found", response = ErrorResponse.class)
    })
    public Response deleteExperiment(
            @ApiParam(value = EXPERIMENT_API_VALUE, example = EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI xpUri
    ) throws Exception {
        ExperimentDAO dao = new ExperimentDAO(sparql, nosql);
        dao.delete(xpUri, currentUser);
        return new ObjectUriResponse(xpUri).getResponse();
    }

    @GET
    @Path("{uri}/available_facilities")
    @ApiOperation("Get facilities available for an experiment")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return facilities list", response = FacilityGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })
    public Response getAvailableFacilities(
            @ApiParam(value = EXPERIMENT_API_VALUE, example = EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI xpUri
    ) throws Exception {
        ExperimentDAO xpDao = new ExperimentDAO(sparql, nosql);
        List<FacilityModel> facilities = xpDao.getAvailableFacilities(xpUri, currentUser);

        List<FacilityGetDTO> dtoList = facilities.stream().map((item) -> FacilityGetDTO.getDTOFromModel(item, false)).collect(Collectors.toList());
        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @GET
    @Path("{uri}/species")
    @ApiOperation("Get species present in an experiment")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return Species list", response = SpeciesDTO.class, responseContainer = "List")
    })
    public Response getAvailableSpecies(
            @ApiParam(value = EXPERIMENT_API_VALUE, example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI xpUri
    ) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql, nosql);
        xpDAO.validateExperimentAccess(xpUri, currentUser);

        SpeciesDAO dao = new SpeciesDAO(sparql);
        List<SpeciesModel> species = dao.getByExperiment(xpUri, currentUser.getLanguage());

        List<SpeciesDTO> dtoList = species.stream().map(SpeciesDTO::fromModel).collect(Collectors.toList());
        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @GET
    @Path("{uri}/factors")
    @ApiOperation("Get factors with their levels associated to an experiment")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return factors list", response = FactorDetailsGetDTO.class, responseContainer = "List")
    })
    public Response getAvailableFactors(
            @ApiParam(value = EXPERIMENT_API_VALUE, example = ExperimentAPI.EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI xpUri
    ) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql, nosql);
        xpDAO.validateExperimentAccess(xpUri, currentUser);

        FactorDAO dao = new FactorDAO(sparql);
        List<FactorModel> factors = dao.getByExperiment(xpUri, currentUser.getLanguage());

        List<FactorDetailsGetDTO> dtoList = factors.stream().map(FactorDetailsGetDTO::fromModel).collect(Collectors.toList());
        return new PaginatedListResponse<>(dtoList).getResponse();
    }

    @GET
    @Path("by_uris")
    @ApiOperation("Get experiments URIs")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return experiments", response = ExperimentGetListDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Experiment not found (if any provided URIs is not found", response = ErrorDTO.class)
    })
    public Response getExperimentsByURIs(
            @ApiParam(value = "Experiments URIs", required = true) @QueryParam("uris") @NotNull List<URI> uris
    ) throws Exception {
        ExperimentDAO dao = new ExperimentDAO(sparql, nosql);
        List<ExperimentModel> models = dao.getByURIs(uris, currentUser);

        if (!models.isEmpty()) {
            List<ExperimentGetListDTO> resultDTOList = new ArrayList<>(models.size());
            models.forEach(result -> {
                resultDTOList.add(ExperimentGetListDTO.fromModel(result));
            });

            return new PaginatedListResponse<>(resultDTOList).getResponse();
        } else {
            // Otherwise return a 404 - NOT_FOUND error response
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Experiments not found",
                    "Unknown experiment URIs"
            ).getResponse();
        }
    }

}
