//******************************************************************************
//                          CallsResourceService.java 
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 24 Sept. 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.brapi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.view.brapi.Status;
import opensilex.service.view.brapi.form.BrapiMultiResponseForm;
import opensilex.service.model.Call;
import java.util.ServiceLoader;


@Api("/brapi/v1/calls")
@Path("/brapi/v1/calls")
/**
 * Calls resource service.
 * @see https://brapi.docs.apiary.io/#reference/calls/call-search
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class CallsResourceService implements BrapiCall {
  
    /**
     * Overriding BrapiCall method.
     * @return Calls call information
     */
    @Override
    public ArrayList<Call> callInfo() {
        ArrayList<Call> calls = new ArrayList();
        ArrayList<String> calldatatypes = new ArrayList<>();
        calldatatypes.add("json");
        ArrayList<String> callMethods = new ArrayList<>();
        callMethods.add("GET");
        ArrayList<String> callVersions = new ArrayList<>();
        callVersions.add("1.3");
        Call call = new Call("calls", calldatatypes, callMethods, callVersions);
        calls.add(call);
        return calls;
    }
    


    /**
     * Calls GET service.
     * @param dataType
     * @param limit
     * @param page
     * @return BrAPI calls list
     * @example
     * [ 
     *   { 
     *     call1 description 
     *   }, 
     *   { 
     *     call2 description
     *   }, 
     * ]
     */
    @GET
    @ApiOperation(value = "Check the available BrAPI calls",
            notes = "Check the available BrAPI calls")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve BrAPI calls", response = Call.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)})

    @Produces(MediaType.APPLICATION_JSON)
    public Response getCalls(
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
            @ApiParam(value = DocumentationAnnotation.CALL_DATATYPE_DEFINITION, example = DocumentationAnnotation.EXAMPLE_CALL_DATATYPE) @QueryParam("dataType") @Pattern(regexp = "json") String dataType) {

        ArrayList<Status> statusList = new ArrayList();
        ArrayList<Call> callsInfoList = new ArrayList();
        
        /**
         * get all BrapiCalls callInfo() outputs.
         */
        ServiceLoader.load(BrapiCall.class, getClass().getClassLoader()).forEach((BrapiCall service) -> {
            ArrayList<Call> calls = service.callInfo();
            ArrayList<String> datatypesList = new ArrayList();
            calls.forEach((call) -> {
                datatypesList.addAll(call.getDataTypes());
            });            
            if (dataType == null || datatypesList.contains(dataType) == true) {
                callsInfoList.addAll(calls);
            }
        });

        BrapiMultiResponseForm getResponse = new BrapiMultiResponseForm(limit, page, callsInfoList, false);
        getResponse.getMetadata().setStatus(statusList);
        return Response.status(Response.Status.OK).entity(getResponse).build();
    }
}
