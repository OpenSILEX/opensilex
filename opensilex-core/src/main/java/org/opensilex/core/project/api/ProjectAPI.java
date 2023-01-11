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
import org.opensilex.core.experiment.api.ExperimentGetDTO;
import org.opensilex.core.project.dal.ProjectDAO;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.response.ErrorResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.security.authentication.ApiCredential;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.ErrorDTO;
import org.opensilex.sparql.exceptions.SPARQLAlreadyExistingUriException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 * @author Julien BONNEFONT
 */
@Api(ProjectAPI.CREDENTIAL_PROJECT_GROUP_ID)
@Path("/core/projects")
@ApiCredentialGroup(
        groupId = ProjectAPI.CREDENTIAL_PROJECT_GROUP_ID,
        groupLabelKey = ProjectAPI.CREDENTIAL_PROJECT_GROUP_LABEL_KEY
)
public class ProjectAPI {

    public static final String CREDENTIAL_PROJECT_GROUP_ID = "Projects";
    public static final String CREDENTIAL_PROJECT_GROUP_LABEL_KEY = "credential-groups.projects";

    public static final String CREDENTIAL_PROJECT_MODIFICATION_ID = "project-modification";
    public static final String CREDENTIAL_PROJECT_MODIFICATION_LABEL_KEY = "credential.default.modification";

    public static final String CREDENTIAL_PROJECT_DELETE_ID = "project-delete";
    public static final String CREDENTIAL_PROJECT_DELETE_LABEL_KEY = "credential.default.delete";

    protected static final String PROJECT_EXAMPLE_URI = "http://opensilex/set/project/BW1";

    @CurrentUser
    AccountModel currentUser;

    @Inject
    private SPARQLService sparql;

    /**
     * Create a Project
     *
     * @param dto the Project to create
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the created Project {@link URI}
     * @throws java.lang.Exception
     */
    @POST
    @ApiOperation("Add a project")
    @ApiProtected
    @ApiCredential(
            credentialId = CREDENTIAL_PROJECT_MODIFICATION_ID,
            credentialLabelKey = CREDENTIAL_PROJECT_MODIFICATION_LABEL_KEY
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "A project is created", response = ObjectUriResponse.class),
        @ApiResponse(code = 409, message = "A project with the same URI already exists", response = ErrorResponse.class)
    })

    public Response createProject(
            @ApiParam("Project description") @Valid ProjectCreationDTO dto
    ) throws Exception {
        try {
            ProjectDAO dao = new ProjectDAO(sparql);
            ProjectModel model = dto.newModel();
            model.setCreator(currentUser.getUri());

            model = dao.create(model);
            return new ObjectUriResponse(Response.Status.CREATED, model.getUri()).getResponse();
        } catch (SPARQLAlreadyExistingUriException e) {
            // Return error response 409 - CONFLICT if project URI already exists
            return new ErrorResponse(
                    Response.Status.CONFLICT,
                    "Project already exists",
                    "Duplicated URI: " + e.getUri()
            ).getResponse();
        }
    }

    /**
     * @param dto the Project to update
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the updated Project {@link URI}
     */
    @PUT
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
        @ApiResponse(code = 404, message = "Unknown Project URI", response = ErrorResponse.class)
    })
    public Response updateProject(
            @ApiParam("Project description") @Valid ProjectCreationDTO dto
    ) throws Exception {
        ProjectDAO dao = new ProjectDAO(sparql);
        ProjectModel model = dao.update(dto.newModel(), currentUser);
        return new ObjectUriResponse(Response.Status.OK, model.getUri()).getResponse();
    }

    /**
     * @param uri the Project URI
     * @return a {@link Response} with a {@link SingleObjectResponse} containing
     * the {@link ExperimentGetDTO}
     */
    @GET
    @Path("{uri}")
    @ApiOperation("Get a project")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Project retrieved", response = ProjectGetDetailDTO.class),
        @ApiResponse(code = 404, message = "Unknown Project URI", response = ErrorResponse.class)
    })
    public Response getProject(
            @ApiParam(value = "Project URI", example = "http://example.com/", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        ProjectDAO dao = new ProjectDAO(sparql);
        ProjectModel model = dao.get(uri, currentUser);
        return new SingleObjectResponse<>(ProjectGetDetailDTO.fromModel(model)).getResponse();
    }

    /**
     * Search projects
     *
     * @param year
     * @param term
     * @param financial
     * @param name
     * @param orderByList
     * @param page
     * @param pageSize
     * @return filtered, ordered and paginated list
     * @throws java.lang.Exception
     * @see ProjectDAO
     */
    @GET
    @ApiOperation("Search projects")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return projects", response = ProjectGetDTO.class, responseContainer = "List")
    })
    public Response searchProjects(
            @ApiParam(value = "Regex pattern for filtering by name or shortname", example = "PJ17") @QueryParam("name") String name,
            @ApiParam(value = "Search by year", example = "2017") @QueryParam("year") Integer year,
            @ApiParam(value = "Regex pattern for filtering on description or objective", example = "climate") @QueryParam("keyword") String term,
            @ApiParam(value = "Regex pattern for filtering by financial funding", example = "ANR") @QueryParam("financial_funding") String financial,
            @ApiParam(value = "List of fields to sort as an array of fieldName=asc|desc", example = "uri=asc") @DefaultValue("name=asc") @QueryParam("order_by") List<OrderBy> orderByList,
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("page_size") @DefaultValue("20") @Min(0) int pageSize
    ) throws Exception {
        ProjectDAO prjctDao = new ProjectDAO(sparql);
        ListWithPagination<ProjectModel> resultList = prjctDao.search(
                name,
                term,
                financial,
                year,
                currentUser,
                orderByList,
                page,
                pageSize
        );

        // Convert paginated list to DTO
        ListWithPagination<ProjectGetDTO> resultDTOList = resultList.convert(ProjectGetDTO.class, ProjectGetDTO::fromModel);
        return new PaginatedListResponse<>(resultDTOList).getResponse();
    }

    @GET
    @Path("by_uris")
    @ApiOperation("Get projects by their URIs")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return projects", response = ProjectGetDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Invalid parameters", response = ErrorDTO.class),
        @ApiResponse(code = 404, message = "Project not found (if any provided URIs is not found", response = ErrorDTO.class)
    })
    public Response getProjectsByURI(
            @ApiParam(value = "Projects URIs", required = true) @QueryParam("uris") @NotNull List<URI> uris
    ) throws Exception {
        ProjectDAO dao = new ProjectDAO(sparql);
        List<ProjectModel> models = dao.getList(uris, currentUser);

        if (!models.isEmpty()) {
            List<ProjectGetDTO> resultDTOList = new ArrayList<>(models.size());
            models.forEach(result -> {
                resultDTOList.add(ProjectGetDTO.fromModel(result));
            });

            return new PaginatedListResponse<>(resultDTOList).getResponse();
        } else {
            // Otherwise return a 404 - NOT_FOUND error response
            return new ErrorResponse(
                    Response.Status.NOT_FOUND,
                    "Projects not found",
                    "Unknown project URIs"
            ).getResponse();
        }
    }

    /**
     * Remove a project
     *
     * @param uri the project URI
     * @return a {@link Response} with a {@link ObjectUriResponse} containing
     * the deleted Project {@link URI}
     */
    @DELETE
    @Path("{uri}")
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
        @ApiResponse(code = 404, message = "Unknown Project URI", response = ErrorResponse.class)
    })
    public Response deleteProject(
            @ApiParam(value = "Project URI", example = PROJECT_EXAMPLE_URI, required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        ProjectDAO dao = new ProjectDAO(sparql);
        dao.delete(uri, currentUser);
        return new ObjectUriResponse(uri).getResponse();

    }
}
