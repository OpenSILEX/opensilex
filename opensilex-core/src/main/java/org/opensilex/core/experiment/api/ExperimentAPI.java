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
import org.opensilex.core.experiment.dal.ExperimentSearchDTO;
import org.opensilex.rest.authentication.ApiCredential;
import org.opensilex.rest.authentication.ApiProtected;
import org.opensilex.rest.validation.date.DateConstraint;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

/**
 * @author Vincent MIGOT
 * @author Renaud COLIN
 */
@Api(ExperimentAPI.CREDENTIAL_EXPERIMENT_GROUP_ID)
@Path("/core/experiment")
public class ExperimentAPI {

    public static final String CREDENTIAL_EXPERIMENT_GROUP_ID = "Experiments";
    public static final String CREDENTIAL_EXPERIMENT_GROUP_LABEL_KEY = "credential-groups.experiments";

    public static final String CREDENTIAL_EXPERIMENT_MODIFICATION_ID = "experiment-modification";
    public static final String CREDENTIAL_EXPERIMENT_MODIFICATION_LABEL_KEY = "credential.experiment.modification";

    public static final String CREDENTIAL_EXPERIMENT_READ_ID = "experiment-read";
    public static final String CREDENTIAL_EXPERIMENT_READ_LABEL_KEY = "credential.experiment.read";

    public static final String CREDENTIAL_EXPERIMENT_DELETE_ID = "experiment-delete";
    public static final String CREDENTIAL_EXPERIMENT_DELETE_LABEL_KEY = "credential.experiment.delete";

    protected static final String EXPERIMENT_EXAMPLE_URI = "http://opensilex/set/experiments/ZA17";

    @Inject
    public ExperimentAPI(SPARQLService sparql) {
        this.sparql = sparql;
    }

    private final SPARQLService sparql;

    /**
     * Create an Experiment
     *
     * @param xpDto the Experiment to create
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the created Experiment {@link URI}
     */
    @POST
    @Path("create")
    @ApiOperation("Create an experiment")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_EXPERIMENT_GROUP_ID,
            groupLabelKey = CREDENTIAL_EXPERIMENT_GROUP_LABEL_KEY,
            credentialId = CREDENTIAL_EXPERIMENT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_EXPERIMENT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Create an experiment", response = ObjectUriResponse.class),
        @ApiResponse(code = 409, message = "An experiment with the same URI already exists", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response createExperiment(
            @ApiParam("Experiment description") @Valid ExperimentCreationDTO xpDto
    ) {
        try {
            ExperimentDAO dao = new ExperimentDAO(sparql);
            ExperimentModel createdXp = dao.create(xpDto.newModel());
            return new ObjectUriResponse(Response.Status.CREATED, createdXp.getUri()).getResponse();

        } catch (SPARQLAlreadyExistingUriException e) {
            return new ErrorResponse(Response.Status.CONFLICT, "Experiment already exists", e.getMessage()).getResponse();
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }

    /**
     * @param xpDto the Experiment to update
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the updated Experiment {@link URI}
     */
    @PUT
    @Path("update")
    @ApiOperation("Update an experiment")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_EXPERIMENT_GROUP_ID,
            groupLabelKey = CREDENTIAL_EXPERIMENT_GROUP_LABEL_KEY,
            credentialId = CREDENTIAL_EXPERIMENT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_EXPERIMENT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Experiment updated", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Invalid or unknown Experiment URI", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public Response updateExperiment(
            @ApiParam("Experiment description") @Valid ExperimentCreationDTO xpDto
    ) {
        try {
            ExperimentDAO dao = new ExperimentDAO(sparql);

            ExperimentModel model = xpDto.newModel();
            dao.update(model);
            return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();

        } catch (SPARQLInvalidURIException e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "Invalid or unknown Experiment URI", e.getMessage()).getResponse();
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }

    /**
     * @param xpUri the Experiment URI
     * @return a {@link Response} with a {@link SingleObjectResponse} containing
     * the {@link ExperimentGetDTO}
     */
    @GET
    @Path("get/{uri}")
    @ApiOperation("Get an experiment by URI")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_EXPERIMENT_GROUP_ID,
            groupLabelKey = CREDENTIAL_EXPERIMENT_GROUP_LABEL_KEY,
            credentialId = CREDENTIAL_EXPERIMENT_READ_ID,
            credentialLabelKey = CREDENTIAL_EXPERIMENT_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Experiment retrieved", response = ExperimentGetDTO.class),
        @ApiResponse(code = 204, message = "No experiment found", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public Response getExperiment(
            @ApiParam(value = "Experiment URI", example = "http://opensilex.dev/set/experiments/ZA17", required = true) @PathParam("uri") @NotNull URI xpUri
    ) {
        try {
            ExperimentDAO dao = new ExperimentDAO(sparql);
            ExperimentModel model = dao.get(xpUri);

            if (model != null) {
                return new SingleObjectResponse<>(ExperimentGetDTO.fromModel(model)).getResponse();
            } else {
                return new ErrorResponse(
                        Response.Status.NO_CONTENT, "Experiment not found",
                        "Unknown Experiment URI: " + xpUri.toString()
                ).getResponse();
            }
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }

    /**
     * Search experiments
     *
     * @return filtered, ordered and paginated list
     * @see ExperimentDAO
     */
    @GET
    @Path("search")
    @ApiOperation("Search Experiments")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_EXPERIMENT_GROUP_ID,
            groupLabelKey = CREDENTIAL_EXPERIMENT_GROUP_LABEL_KEY,
            credentialId = CREDENTIAL_EXPERIMENT_READ_ID,
            credentialLabelKey = CREDENTIAL_EXPERIMENT_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return Experiment list", response = ExperimentGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 204, message = "No experiment found", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })
    public Response searchExperiments(
            @ApiParam(value = "Search by uri", example = EXPERIMENT_EXAMPLE_URI) @QueryParam("uri") URI uri,
            @ApiParam(value = "Search by start date", example = "2017-06-15") @QueryParam("startDate") @DateConstraint String startDate,
            @ApiParam(value = "Search by end date", example = "2017-06-15") @QueryParam("endDate") @DateConstraint String endDate,
            @ApiParam(value = "Search by campaign", example = "2019") @QueryParam("campaign") Integer campaign,
            @ApiParam(value = "Regex pattern for filtering by label", example = "ZA17") @QueryParam("label") String label,
            @ApiParam(value = "Search by keywords", example = "opensilex \ndigital agriculture") @QueryParam("keywords") List<String> keywords,
            @ApiParam(value = "Regex pattern for filtering by comment", example = ".*") @QueryParam("comment") String comment,
            @ApiParam(value = "Regex pattern for filtering by objective", example = ".*") @QueryParam("objective") String objective,
            @ApiParam(value = "Search by involved species", example = "http://www.phenome-fppn.fr/id/species/zeamays") @QueryParam("species") URI species,
            @ApiParam(value = "Search by related project uri", example = "http://www.phenome-fppn.fr/projects/ZA17\nhttp://www.phenome-fppn.fr/id/projects/ZA18") @QueryParam("projects") List<URI> projects,
            @ApiParam(value = "Search by scientific(s) supervisor(s)", example = "http://www.phenome-fppn.fr/users/anne.tireau\nhttp://www.phenome-fppn.fr/users/pascal.neveu") @QueryParam("scientificSupervisors") List<URI> scientificSupervisors,
            @ApiParam(value = "Search by technical(s) supervisor(s)", example = "http://www.phenome-fppn.fr/users/anne.tireau\nhttp://www.phenome-fppn.fr/users/pascal.neveu") @QueryParam("technicalSupervisors") List<URI> technicalSupervisors,
            @ApiParam(value = "Search by infrastructure(s)") @QueryParam("infrastructures") List<URI> infrastructures,
            @ApiParam(value = "Search by devices(s)") @QueryParam("devices") List<URI> installations,
            @ApiParam(value = "Search by groups(s)") @QueryParam("groups") List<URI> groups,
            @ApiParam(value = "Search by sensor(s)") @QueryParam("sensors") List<URI> sensors,
            @ApiParam(value = "Search by involved variable(s)") @QueryParam("variables") List<URI> variables,
            @ApiParam(value = "Search private(false) or public projects(true)", example = "true") @QueryParam("isPublic") Boolean isPublic,
            @ApiParam(value = "Search ended(false) or active projects(true)", example = "true") @QueryParam("isEnded") Boolean isEnded,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "label=asc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) {

        try {
            ExperimentDAO xpDao = new ExperimentDAO(sparql);

            // set searchDTO specifics attributes
            ExperimentSearchDTO searchDTO = new ExperimentSearchDTO()
                    .setEnded(isEnded);

            // set general experiment DTO attributes
            searchDTO.setUri(uri)
                    .setCampaign(campaign)
                    .setLabel(label)
                    .setSpecies(species)
                    .setStartDate(startDate)
                    .setEndDate(endDate)
                    .setIsPublic(isPublic)
                    .setComment(comment)
                    .setObjective(objective);

            // set list based DTO attributes
            searchDTO.setTechnicalSupervisors(technicalSupervisors)
                    .setScientificSupervisors(scientificSupervisors)
                    .setProjects(projects)
                    .setGroups(groups)
                    .setVariables(variables)
                    .setSensors(sensors)
                    .setKeywords(keywords)
                    .setInfrastructures(infrastructures)
                    .setInstallations(installations);

            ListWithPagination<ExperimentModel> resultList = xpDao.search(searchDTO, orderByList, page, pageSize);
            if (resultList.getList().isEmpty()) {
                return new ErrorResponse(Response.Status.NO_CONTENT, "No experiment found", "").getResponse();
            }
            // Convert paginated list to DTO
            ListWithPagination<ExperimentGetDTO> resultDTOList = resultList.convert(ExperimentGetDTO.class, ExperimentGetDTO::fromModel);
            return new PaginatedListResponse<>(resultDTOList).getResponse();

        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }

    }

    /**
     * Remove an experiment
     *
     * @param xpUri the experiment URI
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the deleted Experiment {@link URI}
     */
    @DELETE
    @Path("delete/{uri}")
    @ApiOperation("Delete an experiment")
    @ApiProtected
    @ApiCredential(
            groupId = CREDENTIAL_EXPERIMENT_GROUP_ID,
            groupLabelKey = CREDENTIAL_EXPERIMENT_GROUP_LABEL_KEY,
            credentialId = CREDENTIAL_EXPERIMENT_DELETE_ID,
            credentialLabelKey = CREDENTIAL_EXPERIMENT_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Experiment deleted", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Invalid or unknown Experiment URI", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public Response deleteExperiment(
            @ApiParam(value = "Experiment URI", example = EXPERIMENT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI xpUri
    ) {
        try {
            ExperimentDAO dao = new ExperimentDAO(sparql);
            dao.delete(xpUri);
            return new ObjectUriResponse(xpUri).getResponse();

        } catch (SPARQLInvalidURIException e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "Invalid or unknown Experiment URI", e.getMessage()).getResponse();
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }
}
