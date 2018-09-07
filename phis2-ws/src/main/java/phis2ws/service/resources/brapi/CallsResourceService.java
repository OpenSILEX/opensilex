//******************************************************************************
//                                       CallsResourceService.java 
// SILEX-PHIS
// Copyright © INRA 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.brapi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import javax.inject.Inject;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.hk2.api.IterableProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.ResponseFormCall;
import phis2ws.service.view.model.phis.Call;

@Api("/brapi/v1/calls")
@Path("/brapi/v1/calls")

/**
 * Calls service
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class CallsResourceService implements BrapiCall {

    final static Logger LOGGER = LoggerFactory.getLogger(CallsResourceService.class);

    /**
     * Overriding BrapiCall method
     * @date 27 Aug 2018
     * @return Calls call information
     */
    @Override
    public Call callInfo() {
        ArrayList<String> calldatatypes = new ArrayList<>();
        calldatatypes.add("json");
        ArrayList<String> callMethods = new ArrayList<>();
        callMethods.add("GET");
        ArrayList<String> callVersions = new ArrayList<>();
        callVersions.add("1.1");
        callVersions.add("1.2");
        Call callscall = new Call("calls", calldatatypes, callMethods, callVersions);
        return callscall;
    }

    /* Dependency injection to get all BrapiCalls callInfo() outputs
     */
    @Inject
    IterableProvider<BrapiCall> brapiCallsList;

    /**
     * @param datatype
     * @param limit
     * @param page
     * @return brapi calls list returns in "data" is like this : [ { call1
     * description }, { call2 description }, ]
     */
    @GET
    @ApiOperation(value = "Check the available brapi calls",
            notes = "Check the available brapi calls")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve brapi calls", response = Call.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)})

    @Produces(MediaType.APPLICATION_JSON)
    public Response getCalls(
            @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int limit,
            @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
            @ApiParam(value = DocumentationAnnotation.CALL_DATATYPE_DEFINITION, example = DocumentationAnnotation.EXAMPLE_CALL_DATATYPE) @QueryParam("datatype") @Pattern(regexp = "json") String datatype) {

        ArrayList<Status> statuslist = new ArrayList();
        ArrayList<Call> callsInfoList = new ArrayList();

        for (BrapiCall bc : brapiCallsList) {
            
            Call callinfo = bc.callInfo();
            ArrayList<String> datatypesList = callinfo.getDatatypes();
            if (datatype == null || datatypesList.contains(datatype) == true) {
                callsInfoList.add(callinfo);
            }
        }

        ResponseFormCall getResponse = new ResponseFormCall(limit, page, callsInfoList, false, statuslist);
        return Response.status(Response.Status.OK).entity(getResponse).build();
    }
}
