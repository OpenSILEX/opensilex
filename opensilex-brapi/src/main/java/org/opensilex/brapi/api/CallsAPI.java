//******************************************************************************
//                          BrapiModule.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import java.util.ServiceLoader;
import javax.validation.constraints.Min;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.brapi.model.Call;
import org.opensilex.security.authentication.ApiCredentialGroup;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.utils.ListWithPagination;

@Api("BRAPI")
@Path("/brapi/v1/calls")
@ApiCredentialGroup(
        groupId = CallsAPI.CREDENTIAL_CALLS_GROUP_ID,
        groupLabelKey = CallsAPI.CREDENTIAL_CALLS_GROUP_LABEL_KEY
)
/**
 * Calls resource service.
 * @see Brapi documentation V1.3 https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3
 * @author Alice Boizet
 */
public class CallsAPI implements BrapiCall {
    public static final String CREDENTIAL_CALLS_GROUP_ID = "brapi-calls";
    public static final String CREDENTIAL_CALLS_GROUP_LABEL_KEY = "credential-group-brapi-calls";
  
    /**
     * Overriding BrapiCall method.
     * @return Calls call information
     */
    @Override
    public ArrayList<Call> callInfo() {
        ArrayList<Call> calls = new ArrayList();
        ArrayList<String> calldatatypes = new ArrayList();
        calldatatypes.add("json");
        ArrayList<String> callMethods = new ArrayList();
        callMethods.add("GET");
        ArrayList<String> callVersions = new ArrayList();
        callVersions.add("1.3");
        Call call = new Call("calls", calldatatypes, callMethods, callVersions);
        calls.add(call);
        return calls;
    } 

    /**
     * Calls GET service.
     * @param dataType
     * @param pageSize
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
        @ApiResponse(code = 200, message = "Retrieve BrAPI calls", response = Call.class, responseContainer = "List")})

    @Produces(MediaType.APPLICATION_JSON)
    public Response getCalls(
            @ApiParam(value = "Page number", example = "0") @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @ApiParam(value = "Page size", example = "20") @QueryParam("pageSize") @DefaultValue("20") @Min(0) int pageSize,
            @ApiParam(value = "datatype", example = "json") @QueryParam("dataType") String dataType) {

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

        ListWithPagination<Call> callsList = new ListWithPagination<Call>(callsInfoList, page, pageSize, callsInfoList.size());
        return new BrapiPaginatedListResponse<>(callsList).getResponse();
    }
}
