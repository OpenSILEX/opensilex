//**********************************************************************************************
//                                       CallsResourceService.java 
//
// Author(s): Alice Boizet
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: July 2018
// Contact: alice.boizet@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  July 2018
// Subject: Represents the calls service
//***********************************************************************************************

package phis2ws.service.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.ResponseFormCalls;
import phis2ws.service.view.model.phis.Calls;

@Api("/brapi/v1/calls")
@Path("/brapi/v1/calls")
public class CallsResourceService {
    final static Logger LOGGER = LoggerFactory.getLogger(CallsResourceService.class);  
      
    /**
     * @param datatype
     * @param limit
     * @param page
     * 
     * @return liste des calls brapi
     *         Le retour (dans "data") est de la forme : 
     *          [
     *              { description du call1 },
     *              { description du call2 },
     *          ]
     */
    @GET
    @ApiOperation(value = "Check the available brapi calls",
                       notes = "Check the available brapi calls")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve brapi calls", response = Calls.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)})
    
    @Produces(MediaType.APPLICATION_JSON)
    
    public Response getCalls(
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int limit,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page,
        //@ApiParam(value = "Search by start date", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_START_DATE) @QueryParam("startDate") String startDate,
        @ApiParam(value = "The data type supported by the call, example: json") @QueryParam("datatype") String datatype){
               
        /*String[][] calls= {
                            {"token","json",{"POST","DELETE"},{"1.1","1.2"}},
                            {"calls","json",{"GET"},{"1.1","1.2"}}
                           };
        */

        ArrayList<Calls> callslist = new ArrayList<>();
        
        ArrayList<String> call1datatypes = new ArrayList<>();        
        call1datatypes.add("json");
        
        ArrayList<String> call1Methods = new ArrayList<>();  
        call1Methods.add("GET");
        call1Methods.add("DELETE");
        
        ArrayList<String> call1Versions = new ArrayList<>();  
        call1Versions.add("1.1");
        call1Versions.add("1.2");
        
        
        ArrayList<String> call2datatypes = new ArrayList<>();        
        call2datatypes.add("json");
        ArrayList<String> call2Methods = new ArrayList<>();  
        call2Methods.add("GET");
        ArrayList<String> call2Versions = new ArrayList<>();  
        call2Versions.add("1.1");
        call2Versions.add("1.2");
                
        
        Calls call1 = new Calls("token", call1datatypes, call1Methods, call1Versions);
        Calls call2 = new Calls("calls", call2datatypes, call2Methods, call2Versions);
//        Calls call2 = new Calls("calls", Arrays.asList("json"), Arrays.asList("GET"), Arrays.asList("1.1","1.2"));
        callslist.add(call1);
        callslist.add(call2);
        
        ArrayList<Status> statuslist = new ArrayList();
        
        ResponseFormCalls  getResponse = new ResponseFormCalls(0, 0, callslist, true, statuslist);
       
        return Response.status(Response.Status.OK).entity(getResponse).build();
        }   
    
}
