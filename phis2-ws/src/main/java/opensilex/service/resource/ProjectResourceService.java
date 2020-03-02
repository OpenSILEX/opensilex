//******************************************************************************
//                           ProjectResourceService.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: March 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.apache.commons.lang3.StringUtils;
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import opensilex.service.result.ResultForm;
import opensilex.service.resource.dto.project.ProjectDTO;
import opensilex.service.resource.dto.project.ProjectDetailDTO;
import opensilex.service.resource.dto.project.ProjectPostDTO;
import opensilex.service.resource.dto.project.ProjectPutDTO;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.project.dal.ProjectDAO;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.rest.authentication.ApiProtected;
import org.opensilex.rest.authentication.AuthenticationService;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.sparql.utils.OrderBy;
import org.opensilex.utils.ListWithPagination;

/**
 * Project resource service.
 *
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/projects")
@Path("projects")
public class ProjectResourceService extends ResourceService {

    public static final Property hasFinancialFunding = Ontology.property(Oeso.NS, "hasFinancialFunding");
    public static final Property hasFinancialReference = Ontology.property(Oeso.NS, "hasFinancialReference");
    public static final Resource FinancialFunding = Ontology.resource(Oeso.NS, "FinancialFunding");

    @Inject
    private SPARQLService sparql;

    @Inject
    private AuthenticationService authentication;

    /**
     * Service to insert projects.
     *
     * @param projects
     * @param context
     * @example [ { "name": "projectTest", "shortname": "P T",
     * "relatedProjects": [ "http://www.opensilex.org/opensilex/DROPS" ],
     * "financialFunding": http://www.opensilex.org/vocabulary/oeso#inra",
     * "financialReference": "financial reference", "description": "This project
     * is about maize.", "startDate": "2016-07-07", "endDate": "2015-07-07",
     * "keywords": [ "maize" ], "homePage": "http://example.com", "objective":
     * "This is the objective of the project." } ]
     * @return the creation result.
     */
    @POST
    @ApiOperation(value = "Post a project",
            notes = "Register a new project in the database")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Project saved", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)})
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(
            @ApiParam(value = DocumentationAnnotation.PROJECT_POST_DATA_DEFINITION) @Valid ArrayList<ProjectPostDTO> projects,
            @Context HttpServletRequest context,
            @Context SecurityContext securityContext) throws Exception {
        AbstractResultForm postResponse = null;

        if (projects != null && !projects.isEmpty()) {
            UserModel user = (UserModel) securityContext.getUserPrincipal();
            ProjectDAO projectDAO = new ProjectDAO(sparql, user.getLang());

            List<ProjectModel> projectModels = new ArrayList<>();
            for (ProjectPostDTO project : projects) {
                projectModels.add(project.getProjectModel(sparql, user.getLang()));
            };

            projectDAO.create(projectModels);

            List<String> uriList = new ArrayList<>();
            for (ProjectModel projectModel : projectModels) {
                uriList.add(projectModel.getUri().toString());
            }

            postResponse = new ResponseFormPOST(new ArrayList<Status>());
            postResponse.getMetadata().setDatafiles(uriList);
            return Response.status(Response.Status.CREATED).entity(postResponse).build();

        } else {
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Empty project(s) to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }

    /**
     * Update projects.
     *
     * @param projects
     * @param context
     * @example [ { "uri": "http://www.opensilex.org/opensilex/PTsf", "name":
     * "projectTest", "shortname": "P T", "relatedProjects": [
     * "http://www.opensilex.org/opensilex/DROPS" ], "financialFunding":
     * "http://www.opensilex.org/vocabulary/oeso#inra", "financialReference":
     * "financial reference", "description": "This project is about maize.",
     * "startDate": "2016-07-07", "endDate": "2015-07-07", "keywords": [ "maize"
     * ], "homePage": "http://example.com", "objective": "This is the objective
     * of the project." } ]
     * @return The update result.
     */
    @PUT
    @ApiOperation(value = "Put project(s)",
            notes = "Update project(s) in the database")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "project(s) updated", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)
    })
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(
            @ApiParam(value = DocumentationAnnotation.PROJECT_POST_DATA_DEFINITION) @Valid ArrayList<ProjectPutDTO> projects,
            @Context HttpServletRequest context,
            @Context SecurityContext securityContext) throws Exception {
        AbstractResultForm putResponse = null;

        UserModel user = (UserModel) securityContext.getUserPrincipal();
        ProjectDAO projectDAO = new ProjectDAO(sparql, user.getLang());

        List<ProjectModel> projectModels = new ArrayList<>();
        SPARQLClassObjectMapper<SPARQLResourceModel> projectMapper = SPARQLClassObjectMapper.getForClass(ProjectModel.class);
        for (ProjectPutDTO project : projects) {
            ProjectModel projectModel = project.getProjectModel(sparql, user.getLang());
            projectModels.add(projectModel);

            if (project.getFinancialReference() != null && !project.getFinancialReference().isEmpty()) {
                sparql.updateObjectRelation(
                        projectMapper.getDefaultGraph(),
                        projectModel.getUri(),
                        hasFinancialReference,
                        new URI(project.getFinancialReference())
                );
            }

            if (project.getFinancialFunding() != null && !project.getFinancialFunding().isEmpty()) {
                sparql.updateObjectRelation(
                        projectMapper.getDefaultGraph(),
                        projectModel.getUri(),
                        hasFinancialFunding,
                        new URI(project.getFinancialFunding())
                );
            }

        };

        projectDAO.update(projectModels);

        List<String> uriList = new ArrayList<>();
        for (ProjectModel projectModel : projectModels) {
            uriList.add(projectModel.getUri().toString());
        }

        putResponse = new ResponseFormPOST(new ArrayList<Status>());
        putResponse.getMetadata().setDatafiles(uriList);
        return Response.status(Response.Status.OK).entity(putResponse).build();
    }

    /**
     * Get the details of a project.
     *
     * @param uri the URI of the project.
     * @param pageSize
     * @param page
     * @return The detail of the project.
     * @example { "metadata": { "pagination": null, "status": [], "datafiles":
     * [] }, "result": { "data": [ { "uri":
     * "http://www.opensilex.org/opensilex/DROPS", "name": "projectTest",
     * "shortname": "P T", "relatedProjects": [], "financialFunding": null,
     * "financialReference": null, "description": "This project is about
     * maize.", "startDate": "2015-07-07", "endDate": "2016-07-07", "keywords":
     * [], "homePage": "http://example.com", "administrativeContacts": [],
     * "coordinators": [], "scientificContacts": [], "objective": "This is the
     * objective of the project." } ] } }
     */
    @GET
    @Path("{uri}")
    @ApiOperation(value = "Get a project by its URI.",
            notes = "Retrieve the project authorized for the user corresponding to theU RI given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve the project", response = ProjectDetailDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@ApiParam(value = DocumentationAnnotation.PROJECT_URI_DEFINITION, example = DocumentationAnnotation.EXAMPLE_PROJECT_URI, required = true) @PathParam("uri") @URL @Required String uri,
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
            @Context SecurityContext securityContext) {
        ArrayList<ProjectDetailDTO> projects = new ArrayList<>();
        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<ProjectDetailDTO> getResponse;

        try {
            UserModel user = (UserModel) securityContext.getUserPrincipal();
            ProjectDAO projectDAO = new ProjectDAO(sparql, user.getLang());

            ProjectModel project = projectDAO.get(new URI(uri));
            ProjectDetailDTO projectDTO = new ProjectDetailDTO(project, sparql, user.getLang());
            projects.add(projectDTO);

            getResponse = new ResultForm<>(pageSize, page, projects, true, 1);
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        } catch (NotFoundException ex) {
            getResponse = new ResultForm<>(0, 0, projects, true);
            return noResultFound(getResponse, statusList);
        } catch (Exception ex) {
            statusList.add(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, ex.getMessage()));
            getResponse = new ResultForm<>(0, 0, projects, true);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(getResponse).build();
        }
    }

    /**
     * Get projects corresponding to the given search params.
     *
     * @param pageSize
     * @param page
     * @param uri
     * @param name
     * @param shortname
     * @param financialFunding
     * @param financialReference
     * @param financialFundingLang
     * @param description
     * @param startDate
     * @param endDate
     * @param homePage
     * @param objective
     * @return the projects corresponding to the search params.
     * @example { "metadata": { "pagination": { "pageSize": 20, "currentPage":
     * 0, "totalCount": 2, "totalPages": 1 }, "status": [], "datafiles": [] },
     * "result": { "data": [ { "uri":
     * "http://www.opensilex.org/opensilex/2019/a19012", "name":
     * "http://www.opensilex.org/opensilex/2019/a19012", "shortname": "DROPS",
     * "financialFunding": { "uri":
     * "http://www.opensilex.org/vocabulary/oeso#inra", "label": "INRA" },
     * "financialReference": "financial reference", "description": "This project
     * is about maize.", "startDate": "2016-07-07", "endDate": "2016-08-07",
     * "homePage": "http://example.com", "objective": "This is the objective of
     * the project." }, { "uri": "http://www.opensilex.org/opensilex/DROPS",
     * "name": "http://www.opensilex.org/opensilex/DROPS", "shortname": "P T",
     * "financialFunding": null, "financialReference": null, "description":
     * "This project is about maize.", "startDate": "2015-07-07", "endDate":
     * "2016-07-07", "homePage": "http://example.com", "objective": "This is the
     * objective of the project." } ] } }
     */
    @GET
    @ApiOperation(value = "Get all projects corresponding to the searched params given",
            notes = "Retrieve all projects authorized for the user corresponding to the searched params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all projects", response = ProjectDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)})
    @ApiProtected
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBySearch(
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
            @ApiParam(value = "Search by URI", example = DocumentationAnnotation.EXAMPLE_PROJECT_URI) @QueryParam("uri") String uri,
            @ApiParam(value = "Search by project name", example = DocumentationAnnotation.EXAMPLE_PROJECT_NAME) @QueryParam("name") String name,
            @ApiParam(value = "Search by shortname", example = DocumentationAnnotation.EXAMPLE_PROJECT_SHORTNAME) @QueryParam("shortname") String shortname,
            @ApiParam(value = "Search by financial funding", example = DocumentationAnnotation.EXAMPLE_PROJECT_FINANCIAL_FUNDING) @QueryParam("financialFunding") String financialFunding,
            @ApiParam(value = "Search by financial funding lang", example = DocumentationAnnotation.EXAMPLE_DOCUMENT_LANGUAGE) @QueryParam("financialFundingLang") String financialFundingLang,
            @ApiParam(value = "Search by financial reference", example = DocumentationAnnotation.EXAMPLE_PROJECT_FINANCIAL_REFERENCE) @QueryParam("financialReference") String financialReference,
            @ApiParam(value = "Search by description", example = DocumentationAnnotation.EXAMPLE_PROJECT_DESCRIPTION) @QueryParam("description") String description,
            @ApiParam(value = "Search by start date", example = DocumentationAnnotation.EXAMPLE_PROJECT_DATE_START) @QueryParam("startDate") String startDate,
            @ApiParam(value = "Search by end date", example = DocumentationAnnotation.EXAMPLE_PROJECT_DATE_END) @QueryParam("endDate") String endDate,
            @ApiParam(value = "Search by home page", example = DocumentationAnnotation.EXAMPLE_PROJECT_HOME_PAGE) @QueryParam("homePage") String homePage,
            @ApiParam(value = "Search by objective", example = DocumentationAnnotation.EXAMPLE_PROJECT_OBJECTIVE) @QueryParam("objective") String objective,
            @Context SecurityContext securityContext) throws Exception {

        UserModel user = (UserModel) securityContext.getUserPrincipal();
        ProjectDAO projectDAO = new ProjectDAO(sparql, user.getLang());

        if (StringUtils.isEmpty(financialFundingLang)) {
            financialFundingLang = DEFAULT_LANGUAGE;
        }

        //1. Get projects
        ListWithPagination<ProjectModel> results = projectDAO.search(uri, name, shortname, description, startDate, endDate, homePage, objective, new ArrayList<OrderBy>(), page, pageSize);

        ArrayList<Status> statusList = new ArrayList<>();
        ResultForm<ProjectDTO> getResponse;
        if (results.getTotal() == 0) {
            getResponse = new ResultForm<>(0, 0, new ArrayList<ProjectDTO>(), true);
            return noResultFound(getResponse, statusList);
        } else {

            ArrayList<ProjectDTO> projectsToReturn = new ArrayList<ProjectDTO>();
            for (ProjectModel project : results.getList()) {
                ProjectDTO projectDTO = new ProjectDTO(project, sparql, user.getLang());
                projectsToReturn.add(projectDTO);
            }

            getResponse = new ResultForm<ProjectDTO>(results.getPageSize(), results.getPage(), projectsToReturn, true, results.getTotal());
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }
    }
}
