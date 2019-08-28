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
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.configuration.GlobalWebserviceValues;
import opensilex.service.dao.DataAnalysisDAO;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.view.brapi.Status;
import opensilex.service.result.ResultForm;
import opensilex.service.view.brapi.form.ResponseFormPOST;

/**
 * DataAnalysis resource service.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
@Api("/dataAnalysis")
@Path("/dataAnalysis")
public class DataAnalysisResourceService extends ResourceService {
    

    /**
     * Call R function via OpenCPU Server
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
        @ApiResponse(code = 200, message = "Retrieve data from R call", response = JsonObject.class, responseContainer = "List")
        ,
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION)
        ,
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED)
        ,
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
            @ApiParam(required = true) @QueryParam("packageName" ) @DefaultValue("stats") @NotEmpty String packageName,
            @ApiParam(required = true) @QueryParam("functionName") @DefaultValue("rnorm") @NotEmpty String functionName,
            @QueryParam("jsonParameters") @DefaultValue("{\"n\":1000}") String jsonParameters) {
               
        Response functionCallResponse = DataAnalysisDAO.opencpuRFunctionProxyCall(packageName, functionName, jsonParameters);
        
        ResultForm<JsonElement> getResponse;
        ArrayList<JsonElement> resultValues = null;
        
        JsonElement jsonResult = null;
        String stringResult = functionCallResponse.readEntity(String.class);
        
        if (functionCallResponse.getStatus() != Response.Status.CREATED.getStatusCode()) {
            ResponseFormPOST noReponse = new ResponseFormPOST(new Status(StatusCodeMsg.ERR, StatusCodeMsg.BAD_DATA_FORMAT, stringResult));
            return Response.status(functionCallResponse.getStatus()).entity(noReponse).build();
        }else{
            jsonResult = new JsonParser().parse(stringResult);
            resultValues = new ArrayList<>();
            resultValues.add(jsonResult);
            getResponse = new ResultForm<> (0, 0, resultValues, true);
            return Response.status(Response.Status.CREATED).entity(getResponse).build();
        }
    }
}
