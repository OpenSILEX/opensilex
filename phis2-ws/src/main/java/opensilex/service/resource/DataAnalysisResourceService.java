//******************************************************************************
//                            DataAnalysisResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 18 June 2018
// Contact: morgane.vidal@inra.fr, arnaud.charleroy@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.dao.RDAO;
import opensilex.service.dao.ScientificAppDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.model.ScientificAppDescription;
import opensilex.service.view.brapi.Status;
import opensilex.service.result.ResultForm;
import opensilex.service.resource.dto.ScientificAppDTO;
import opensilex.service.shinyProxy.ShinyProxyService;
import opensilex.service.view.brapi.form.ResponseFormGET;
import opensilex.service.view.brapi.form.ResponseFormPOST;

/**
 * DataAnalysis resource service.
 *
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
@Api("/dataAnalysis")
@Path("/dataAnalysis")
public class DataAnalysisResourceService extends ResourceService {

    /**
     * Call R function via OpenCPU Server
     *
     * @param packageName R package name
     * @param functionName function Name
     * @param jsonParameters function parameters in Json
     * @return Response
     * @example
     */
    @POST
    @Path("R")
    @ApiOperation(value = "Get a json value",
            notes = "Retrieve json object response from a R call")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Retrieve data from R call", response = JsonObject.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRfunctionResults(
            @ApiParam(required = true) @QueryParam("packageName") @DefaultValue("stats") @NotEmpty String packageName,
            @ApiParam(required = true) @QueryParam("functionName") @DefaultValue("rnorm") @NotEmpty String functionName,
            @QueryParam("jsonParameters") String jsonParameters) {

        RDAO rDao = new RDAO();
        Response functionCallResponse = rDao.opencpuRFunctionProxyCall(packageName, functionName, jsonParameters);

        ResultForm<JsonElement> getResponse;
        ArrayList<JsonElement> resultValues = null;

        JsonElement jsonResult = null;
        String stringResult = functionCallResponse.readEntity(String.class);

        if (functionCallResponse.getStatus() != Response.Status.CREATED.getStatusCode()) {
            ResponseFormPOST noReponse = new ResponseFormPOST(new Status(StatusCodeMsg.ERR, StatusCodeMsg.BAD_DATA_FORMAT, stringResult));
            return Response.status(functionCallResponse.getStatus()).entity(noReponse).build();
        } else {
            jsonResult = new JsonParser().parse(stringResult);
            resultValues = new ArrayList<>();
            resultValues.add(jsonResult);
            getResponse = new ResultForm<>(0, 0, resultValues, true);
            return Response.status(Response.Status.CREATED).entity(getResponse).build();
        }
    }

    /**
     * Shiny Proxy Server Status
     *
     * @return Response
     * @example
     */
    @GET
    @Path("shinyServerStatus")
    @ApiOperation(value = "Get a server status message")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Server Running"),
        @ApiResponse(code = 201, message = "Server Updating"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 404, message = "Not running"),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response shinyProxyServerStatus() {
        ResponseFormGET response;
        Response.Status responseCode;
        if (ShinyProxyService.SHINYPROX_RUN_STATE) {
            response = new ResponseFormGET(new Status(StatusCodeMsg.INFO, "Running", null));
            responseCode = Response.Status.OK;
            if (ShinyProxyService.SHINYPROXY_UPDATE_APP_STATE) {
                response = new ResponseFormGET(new Status(StatusCodeMsg.INFO, "Updating app", null));
                responseCode = Response.Status.CREATED;
            }
        } else {
            response = new ResponseFormGET(new Status(StatusCodeMsg.INFO, "Not Running", null));
            responseCode = Response.Status.SERVICE_UNAVAILABLE;
        }

        return Response.status(responseCode).entity(response).build();
    }

    /**
     * Shiny Proxy Server Status
     *
     * @param limit
     * @param page
     * @return Response
     * @example
     */
    @GET
    @Path("applications")
    @ApiOperation(value = "Get a server availables apps")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "App List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = GlobalWebserviceValues.AUTHORIZATION, required = true,
                dataType = GlobalWebserviceValues.DATA_TYPE_STRING, paramType = GlobalWebserviceValues.HEADER,
                value = DocumentationAnnotation.ACCES_TOKEN,
                example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response shinyProxyServerAppList(
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page) {
        ScientificAppDAO scientificAppDAO = new ScientificAppDAO();
        scientificAppDAO.session = userSession;
        ArrayList<ScientificAppDescription> shinyProxyAppList = scientificAppDAO.find(null, null);
        ArrayList<ScientificAppDTO> shinyProxyAppDTOList = new ArrayList<>();
        for (ScientificAppDescription scientificApplicationDescription : shinyProxyAppList) {
            shinyProxyAppDTOList.add(new ScientificAppDTO(scientificApplicationDescription));
        }

        if (shinyProxyAppDTOList.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).entity(new ResponseFormGET()).build();
        }
        return Response
                .status(Response.Status.OK)
                .entity(new ResultForm<>(limit, page, shinyProxyAppDTOList, false))
                .build();
    }
}
