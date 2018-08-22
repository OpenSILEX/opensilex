//******************************************************************************
//                                       StudiesSearchResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 22 août 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.brapi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.authentication.Session;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.phis.StudyDAO;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.ResponseFormStudiesSearch;
import phis2ws.service.view.model.phis.Call;
import phis2ws.service.view.model.phis.StudiesSearch;

@Api("/brapi/v1/studies-search")
@Path("/brapi/v1/studies-search")

/**
 * Studies Search service
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class StudiesSearchResourceService implements BrapiCall {

    final static Logger LOGGER = LoggerFactory.getLogger(StudiesSearchResourceService.class);
    //User session
    @SessionInject
    Session userSession;

    /**
     * Overriding BrapiCall method
     * @date 22 Aug 2018
     * @return studies-search call information
     */
    @Override
    public Call callInfo() {
        ArrayList<String> calldatatypes = new ArrayList<>();
        calldatatypes.add("json");
        ArrayList<String> callMethods = new ArrayList<>();
        callMethods.add("GET");
        ArrayList<String> callVersions = new ArrayList<>();
        callVersions.add("1.2");
        Call callscall = new Call("studies-search", calldatatypes, callMethods, callVersions);
        return callscall;
    }

    @GET
    @ApiOperation(value = "Get list of studies", notes = "Get list of studies")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Get list of studies", response = StudiesSearch.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)})

    @ApiImplicitParams({
        @ApiImplicitParam(name = "Authorization", required = true,
                dataType = "string", paramType = "header",
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })

    @Produces(MediaType.APPLICATION_JSON)

    public Response getStudies(
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int limit,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page,
            @ApiParam(value = "Search by studyDbId", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI) @QueryParam("studyDbId") String studyDbId,
            @ApiParam(value = "Search by trialDbId", example = "") @QueryParam("trialDbId") String trialDbId,
            @ApiParam(value = "Search by program", example = DocumentationAnnotation.EXAMPLE_PROJECT_URI) @QueryParam("programDbId") String programDbId,
            @ApiParam(value = "Search by crop name from the Experiment", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_CROP_SPECIES) @QueryParam("commonCropName") String commonCropName,
            @ApiParam(value = "Search by location", example = "") @QueryParam("locationDbId") String locationDbId,
            @ApiParam(value = "Search by year", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_CAMPAIGN) @QueryParam("seasonDbId") String seasonDbId,
            @ApiParam(value = "Search by studyType", example = "") @QueryParam("studyType") String studyType,
            @ApiParam(value = "Search by germplasmDbIds") @QueryParam("germplasmDbIds") List<String> germplasmDbIds,
            @ApiParam(value = "Search by observationVariableDbIds", example = DocumentationAnnotation.EXAMPLE_VARIABLE_URI) @QueryParam("observationVariableDbIds") List<String> observationVariableDbIds,
            @ApiParam(value = "Search by active", example = "") @QueryParam("active") Boolean active,
            @ApiParam(value = "sort by", example = "studyDbId") @QueryParam("sortBy") String sortBy,
            @ApiParam(value = "sort order", example = "asc") @QueryParam("sortOrder") String sortOrder
    ) throws SQLException {

        StudyDAO studyDAO = new StudyDAO();

        if (studyType != null) {
            studyDAO.studyType = studyType;
        }

        if (trialDbId != null) {
            studyDAO.trialDbId = trialDbId;
        }

        if (programDbId != null) {
            studyDAO.programDbId = programDbId;
        }

        if (commonCropName != null) {
            studyDAO.commonCropName = commonCropName;
        }

        if (locationDbId != null) {
            studyDAO.locationDbId = locationDbId;
        }

        if (seasonDbId != null) {
            studyDAO.seasonDbId = seasonDbId;
        }

        if (studyDbId != null) {
            studyDAO.studyDbId = studyDbId;
        }

        if (germplasmDbIds != null) {
            studyDAO.germplasmDbIds = germplasmDbIds;
        }

        if (observationVariableDbIds != null) {
            studyDAO.observationVariableDbIds = observationVariableDbIds;
        }

        studyDAO.active = active;

        if (sortBy != null) {
            studyDAO.sortBy = sortBy;
        }

        if (sortOrder != null) {
            studyDAO.sortOrder = sortOrder;
        }

        studyDAO.setPageSize(limit);
        studyDAO.setPage(page);
        studyDAO.user = userSession.getUser();

        return getStudiesData(studyDAO);
    }

    private Response noResultFound(ResponseFormStudiesSearch getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("No results", StatusCodeMsg.INFO, "No results for the studies"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }

    private Response sqlError(ResponseFormStudiesSearch getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("SQL error", StatusCodeMsg.ERR, "can't fetch result"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(getResponse).build();
    }
    
    /**
    * Collect data from a user query (studies research)
    * @param studyDAO StudyDAO
    * @return Response for the user: contains the list of studies corresponding to the query 
    */
    private Response getStudiesData(StudyDAO studyDAO) throws SQLException {
        ArrayList<StudiesSearch> studiesList = new ArrayList<>();
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormStudiesSearch getResponse;
        Integer studiesCount = studyDAO.count();

        if (studiesCount != null && studiesCount == 0) {
            getResponse = new ResponseFormStudiesSearch(studyDAO.getPageSize(), studyDAO.getPage(), studiesList, true);
            return noResultFound(getResponse, statusList);
        } else {
            studiesList = studyDAO.getStudiesList();
            if (studiesList == null) {
                getResponse = new ResponseFormStudiesSearch(0, 0, studiesList, true);
                return sqlError(getResponse, statusList);
            } else if (!studiesList.isEmpty() && studiesCount != null) {
                getResponse = new ResponseFormStudiesSearch(studyDAO.getPageSize(), studyDAO.getPage(), studiesList, false);
                if (getResponse.getResult().dataSize() == 0) {
                    return noResultFound(getResponse, statusList);
                } else {
                    getResponse.setStatus(statusList);
                    return Response.status(Response.Status.OK).entity(getResponse).build();
                }
            } else {
                getResponse = new ResponseFormStudiesSearch(0, 0, studiesList, true);
                return noResultFound(getResponse, statusList);
            }
        }
    }
}
