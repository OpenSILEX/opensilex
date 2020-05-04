//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.project.api;

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
import org.opensilex.core.project.dal.ProjectDAO;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.AuthenticationService;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 * @author vidalmor,Vincent MIGOT,Renaud COLIN
 * @author Julien BONNEFONT
 */
@Api(ProjectAPI.CREDENTIAL_PROJECT_GROUP_ID)
@Path("/core/project")
@ApiCredentialGroup(
        groupId = ProjectAPI.CREDENTIAL_PROJECT_GROUP_ID,
        groupLabelKey = ProjectAPI.CREDENTIAL_PROJECT_GROUP_LABEL_KEY
)
public class ProjectAPI {

    public static final String CREDENTIAL_PROJECT_GROUP_ID = "Projects";
    public static final String CREDENTIAL_PROJECT_GROUP_LABEL_KEY = "credential-groups.projects";

    public static final String CREDENTIAL_PROJECT_MODIFICATION_ID = "project-modification";
    public static final String CREDENTIAL_PROJECT_MODIFICATION_LABEL_KEY = "credential.project.modification";

    public static final String CREDENTIAL_PROJECT_DELETE_ID = "project-delete";
    public static final String CREDENTIAL_PROJECT_DELETE_LABEL_KEY = "credential.project.delete";

    public static final String CREDENTIAL_PROJECT_READ_ID = "project-read";
    public static final String CREDENTIAL_PROJECT_READ_LABEL_KEY = "credential.project.read";

    protected static final String PROJECT_EXAMPLE_URI = "http://opensilex/set/project/BW1";

    @CurrentUser
    UserModel currentUser;

    @Inject
    private SPARQLService sparql;

    @Inject
    private AuthenticationService authentication;

    /**
     * Create a Project
     *
     * @param dto the Project to create
     * @return a {@link Response} with a {@link ObjectUriResponse} containing the created Project {@link URI}
     * @throws java.lang.Exception
     */
    @POST
    @Path("create")
    @ApiOperation("Create a project")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_PROJECT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_PROJECT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Create a project", response = ObjectUriResponse.class),
        @ApiResponse(code = 409, message = "A project with the same URI already exists", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response createProject(
            @ApiParam("Project description") @Valid ProjectCreationDTO dto
    ) throws Exception {
        try {
            ProjectDAO dao = new ProjectDAO(sparql);
            ProjectModel createdPjct = dao.create(dto.newModel());
            return new ObjectUriResponse(Response.Status.CREATED, createdPjct.getUri()).getResponse();

        } catch (SPARQLAlreadyExistingUriException e) {
            return new ErrorResponse(Response.Status.CONFLICT, "Project already exists", e.getMessage()).getResponse();
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }

    /**
     * @param dto the Project to update
     * @return a {@link Response} with a {@link ObjectUriResponse} containing the updated Project {@link URI}
     */
    @PUT
    @Path("update")
    @ApiOperation("Update a project")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_PROJECT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_PROJECT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Project updated", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Invalid or unknown Project URI", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public Response updateProject(
            @ApiParam("Project description") @Valid ProjectCreationDTO dto
    ) {
        try {
            ProjectDAO dao = new ProjectDAO(sparql);
            ProjectModel model = dto.newModel();
            dao.update(model);

            return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();

        } catch (SPARQLInvalidURIException e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "Invalid or unknown Project URI", e.getMessage()).getResponse();
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }

    /**
     * @param prjctUri the Project URI
     * @return a {@link Response} with a {@link SingleObjectResponse} containing the {@link ExperimentGetDTO}
     */
    @GET
    @Path("get/{uri}")
    @ApiOperation("Get a project by URI")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_PROJECT_READ_ID,
            credentialLabelKey = CREDENTIAL_PROJECT_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Project retrieved", response = ProjectGetDTO.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})

    public Response getProject(
            @ApiParam(value = "Project URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI prjctUri
    ) {
        try {
            ProjectDAO dao = new ProjectDAO(sparql);
            ProjectModel model = dao.get(prjctUri, currentUser.getLanguage());

            if (model != null) {
                return new SingleObjectResponse<>(ProjectGetDTO.fromModel(model)).getResponse();
            } else {
                return new ErrorResponse(
                        Response.Status.NO_CONTENT, "Project not found",
                        "Unknown Project URI: " + prjctUri.toString()
                ).getResponse();
            }
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }

    /**
     * Search projects
     *
     * @param startDate
     * @param shortname
     * @param endDate
     * @param financial
     * @param label
     * @param orderByList
     * @param page
     * @param pageSize
     * @return filtered, ordered and paginated list
     * @throws java.lang.Exception
     * @see ProjectDAO
     */
    @GET
    @Path("search")
    @ApiOperation("Search Projects")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_PROJECT_READ_ID,
            credentialLabelKey = CREDENTIAL_PROJECT_READ_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return Project list", response = ProjectGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })
    public Response searchProjects(
            @ApiParam(value = "Search by start date", example = "2017-06-15") @QueryParam("startDate") String startDate,
            @ApiParam(value = "Search by end date", example = "2018-06-15") @QueryParam("endDate") String endDate,
            @ApiParam(value = "Regex pattern for filtering by shortname", example = "PJ17") @QueryParam("shortname") String shortname,
            @ApiParam(value = "Regex pattern for filtering by label", example = "a longer name") @QueryParam("label") String label,
            @ApiParam(value = "Regex pattern for filtering by financial funding", example = "ANR") @QueryParam("financial") String financial,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "label=asc") @QueryParam("orderBy") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {

        try {
            ProjectDAO prjctDao = new ProjectDAO(sparql);
            ListWithPagination<ProjectModel> resultList = prjctDao.search(
                    shortname,
                    label,
                    financial,
                    startDate,
                    endDate,
                    orderByList,
                    page,
                    pageSize
            );

            // Convert paginated list to DTO
            ListWithPagination<ProjectGetDTO> resultDTOList = resultList.convert(ProjectGetDTO.class, ProjectGetDTO::fromModel);
            return new PaginatedListResponse<>(resultDTOList).getResponse();

        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }

    /**
     * Remove a project
     *
     * @param prjctUri the project URI
     * @return a {@link Response} with a {@link ObjectUriResponse} containing the deleted Project {@link URI}
     */
    @DELETE
    @Path("delete/{uri}")
    @ApiOperation("Delete a project")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_PROJECT_DELETE_ID,
            credentialLabelKey = CREDENTIAL_PROJECT_DELETE_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Project deleted", response = ObjectUriResponse.class),
        @ApiResponse(code = 400, message = "Invalid or unknown Project URI", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)})
    public Response deleteProject(
            @ApiParam(value = "Project URI", example = PROJECT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI prjctUri
    ) {
        try {
            ProjectDAO dao = new ProjectDAO(sparql);
            dao.delete(prjctUri);
            return new ObjectUriResponse(prjctUri).getResponse();

        } catch (SPARQLInvalidURIException e) {
            return new ErrorResponse(Response.Status.BAD_REQUEST, "Invalid or unknown Project URI", e.getMessage()).getResponse();
        } catch (Exception e) {
            return new ErrorResponse(e).getResponse();
        }
    }
}
