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
import java.util.ArrayList;
import java.util.List;
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
import org.apache.commons.lang3.StringUtils;
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.dao.ProjectDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.model.Project;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.AbstractResultForm;
import opensilex.service.view.brapi.form.ResponseFormPOST;
import opensilex.service.result.ResultForm;
import opensilex.service.resource.dto.project.ProjectDTO;
import opensilex.service.resource.dto.project.ProjectDetailDTO;
import opensilex.service.resource.dto.project.ProjectPostDTO;
import opensilex.service.resource.dto.project.ProjectPutDTO;

/**
 * Project resource service.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
@Api("/projects")
@Path("projects")
public class ProjectResourceService extends ResourceService {
    /**
     * Transform a list of ProjectPostDTO to a list of Project
     * @param projectsDTOs
     * @return the list of projects
     */
    private List<Project> projectPostDTOsToProjects(List<ProjectPostDTO> projectsDTOs) {
        ArrayList<Project> projects = new ArrayList<>();
        
        for (ProjectPostDTO projectDO : projectsDTOs) {
            projects.add(projectDO.createObjectFromDTO());
        }
        
        return projects;
    }
    
    /**
     * Transform a list of ProjectPutDTO to a list of Project
     * @param projectsDTOs
     * @return the list of projects
     */
    private List<Project> projectPutDTOsToProjects(List<ProjectPutDTO> projectsDTOs) {
        ArrayList<Project> projects = new ArrayList<>();
        
        for (ProjectPutDTO projectDTO : projectsDTOs) {
            projects.add(projectDTO.createObjectFromDTO());
        }
        
        return projects;
    }
    
    /**
     * Service to insert projects.
     * @param projects
     * @param context
     * @example 
     * [
     *      {
     *          "name": "projectTest",
     *          "shortname": "P T",
     *          "relatedProjects": [
     *              "http://www.opensilex.org/opensilex/DROPS"
     *          ],
     *          "financialFunding": http://www.opensilex.org/vocabulary/oeso#inra",
     *          "financialReference": "financial reference",
     *          "description": "This project is about maize.",
     *          "startDate": "2016-07-07",
     *          "endDate": "2015-07-07",
     *          "keywords": [
     *              "maize"
     *          ],
     *          "homePage": "http://example.com",
     *          "objective": "This is the objective of the project."
     *      }
     * ]
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
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(
            @ApiParam(value = DocumentationAnnotation.PROJECT_POST_DATA_DEFINITION) @Valid ArrayList<ProjectPostDTO> projects,
            @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        
        if (projects != null && !projects.isEmpty()) {
            ProjectDAO projectDAO = new ProjectDAO();
            
            POSTResultsReturn result = projectDAO.checkAndInsert(projectPostDTOsToProjects(projects));
            
            if (result.getHttpStatus().equals(Response.Status.CREATED)) {
                postResponse = new ResponseFormPOST(result.statusList);
                postResponse.getMetadata().setDatafiles(result.getCreatedResources());
            } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                    || result.getHttpStatus().equals(Response.Status.OK)
                    || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                postResponse = new ResponseFormPOST(result.statusList);
            }
            return Response.status(result.getHttpStatus()).entity(postResponse).build();
            
        } else {
            postResponse = new ResponseFormPOST(new Status(StatusCodeMsg.REQUEST_ERROR, StatusCodeMsg.ERR, "Empty project(s) to add"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    /**
     * Update projects.
     * @param projects
     * @param context
     * @example
     * [
     *      {
     *          "uri": "http://www.opensilex.org/opensilex/PTsf",
     *          "name": "projectTest",
     *          "shortname": "P T",
     *          "relatedProjects": [
     *              "http://www.opensilex.org/opensilex/DROPS"
     *          ],
     *          "financialFunding": "http://www.opensilex.org/vocabulary/oeso#inra",
     *          "financialReference": "financial reference",
     *          "description": "This project is about maize.",
     *          "startDate": "2016-07-07",
     *          "endDate": "2015-07-07",
     *          "keywords": [
     *              "maize"
     *          ],
     *          "homePage": "http://example.com",
     *          "objective": "This is the objective of the project."
     *      }
     * ]
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
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(
        @ApiParam(value = DocumentationAnnotation.PROJECT_POST_DATA_DEFINITION) @Valid ArrayList<ProjectPutDTO> projects,
        @Context HttpServletRequest context) {
        AbstractResultForm putResponse = null;
        
        ProjectDAO projectDAO = new ProjectDAO();
        POSTResultsReturn result = projectDAO.checkAndUpdate(projectPutDTOsToProjects(projects));
        
        if (result.getHttpStatus().equals(Response.Status.OK)
                || result.getHttpStatus().equals(Response.Status.CREATED)) {
            putResponse = new ResponseFormPOST(result.statusList);
            putResponse.getMetadata().setDatafiles(result.createdResources);
        } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                || result.getHttpStatus().equals(Response.Status.OK)
                || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
            putResponse = new ResponseFormPOST(result.statusList);
        }
        return Response.status(result.getHttpStatus()).entity(putResponse).build();
    }
    
    /**
     * Get the details of a project.
     * @param uri the URI of the project.
     * @param pageSize
     * @param page
     * @return The detail of the project.
     * @example
     * {
     *      "metadata": {
     *          "pagination": null,
     *          "status": [],
     *          "datafiles": []
     *      },
     *      "result": {
     *          "data": [
     *              {
     *                 "uri": "http://www.opensilex.org/opensilex/DROPS",
     *                 "name": "projectTest",
     *                 "shortname": "P T",
     *                 "relatedProjects": [],
     *                 "financialFunding": null,
     *                 "financialReference": null,
     *                 "description": "This project is about maize.",
     *                 "startDate": "2015-07-07",
     *                 "endDate": "2016-07-07",
     *                 "keywords": [],
     *                 "homePage": "http://example.com",
     *                 "administrativeContacts": [],
     *                 "coordinators": [],
     *                 "scientificContacts": [],
     *                 "objective": "This is the objective of the project."
     *              }
     *          ]
     *      }
     * }
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
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@ApiParam(value = DocumentationAnnotation.PROJECT_URI_DEFINITION, example = DocumentationAnnotation.EXAMPLE_PROJECT_URI, required = true) @PathParam("uri") @URL @Required String uri,
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) {
            ArrayList<ProjectDetailDTO> projects = new ArrayList<>();
            ArrayList<Status> statusList = new ArrayList<>();
            ResultForm<ProjectDetailDTO> getResponse;
            
        try {
            ProjectDAO projectDAO = new ProjectDAO();
            
            ProjectDetailDTO project = new ProjectDetailDTO(projectDAO.findById(uri));
            projects.add(project);
            
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
     * @example 
     * {
     *      "metadata": {
     *          "pagination": {
     *              "pageSize": 20,
     *              "currentPage": 0,
     *              "totalCount": 2,
     *              "totalPages": 1
     *          },
     *          "status": [],
     *          "datafiles": []
     *      },
     *      "result": {
     *          "data": [
     *              {
     *                  "uri": "http://www.opensilex.org/opensilex/2019/a19012",
     *                  "name": "http://www.opensilex.org/opensilex/2019/a19012",
     *                  "shortname": "DROPS",
     *                  "financialFunding": {
     *                      "uri": "http://www.opensilex.org/vocabulary/oeso#inra",
     *                      "label": "INRA"
     *                  },
     *                 "financialReference": "financial reference",
     *                 "description": "This project is about maize.",
     *                 "startDate": "2016-07-07",
     *                 "endDate": "2016-08-07",
     *                 "homePage": "http://example.com",
     *                 "objective": "This is the objective of the project."
     *              },
     *            {
     *                 "uri": "http://www.opensilex.org/opensilex/DROPS",
     *                 "name": "http://www.opensilex.org/opensilex/DROPS",
     *                 "shortname": "P T",
     *                 "financialFunding": null,
     *                 "financialReference": null,
     *                 "description": "This project is about maize.",
     *                 "startDate": "2015-07-07",
     *                 "endDate": "2016-07-07",
     *                 "homePage": "http://example.com",
     *                 "objective": "This is the objective of the project."
     *            }
     *          ]
     *      }
     * }
     */
    @GET
    @ApiOperation(value = "Get all projects corresponding to the searched params given",
                  notes = "Retrieve all projects authorized for the user corresponding to the searched params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all projects", response = ProjectDTO.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)})
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
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
        @ApiParam(value = "Search by objective", example = DocumentationAnnotation.EXAMPLE_PROJECT_OBJECTIVE) @QueryParam("objective") String objective)

    {
        
        ProjectDAO projectDAO = new ProjectDAO();     
        if(StringUtils.isEmpty(financialFundingLang))
        	financialFundingLang = DEFAULT_LANGUAGE;
        
        //1. Get count
        Integer totalCount = projectDAO.count(uri, name, shortname, financialFunding, financialReference, description, startDate, endDate, homePage, objective, financialFundingLang);        
        //2. Get projects
        ArrayList<Project> projectsFounded = projectDAO.find(page, pageSize, uri, name, shortname, financialFunding, financialReference, description, startDate, endDate, homePage, objective,financialFundingLang);
        
        //3. Return result
        ArrayList<Status> statusList = new ArrayList<>();
        ArrayList<ProjectDTO> projectsToReturn = new ArrayList<>();
        ResultForm<ProjectDTO> getResponse;
        
        if (projectsFounded == null || projectsFounded.isEmpty()) { //Request failure || No result found
            getResponse = new ResultForm<>(0, 0, projectsToReturn, true);
            return noResultFound(getResponse, statusList);
        } else { //Results
            //Convert all objects to DTOs
            projectsFounded.forEach((project) -> {
                projectsToReturn.add(new ProjectDTO(project));
            });
            
            getResponse = new ResultForm<>(pageSize, page, projectsToReturn, true, totalCount);
            getResponse.setStatus(statusList);
            return Response.status(Response.Status.OK).entity(getResponse).build();
        }
    }
}

