////******************************************************************************
////                       LogResourceService.java
//// SILEX-PHIS
//// Copyright © INRAE
//// Creation date: February 2020
//// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
////******************************************************************************
//package org.opensilex.core.logs.api;
//
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
//import io.swagger.annotations.ApiParam;
//import io.swagger.annotations.ApiResponse;
//import io.swagger.annotations.ApiResponses;
//import java.util.ArrayList;
//import java.util.List;
//import javax.validation.constraints.Min;
//import javax.ws.rs.DefaultValue;
//import javax.ws.rs.GET;
//import javax.ws.rs.Path;
//import javax.ws.rs.Produces;
//import javax.ws.rs.QueryParam;
//import javax.ws.rs.core.MediaType;
//import javax.ws.rs.core.Response;
//
//import opensilex.service.model.DataQueryLog;
//import opensilex.service.resource.dto.data.DataQueryLogSearchDTO;
//
//import org.opensilex.security.authentication.ApiProtected;
//
///**
// * LogResourceService ressource service
// * @author Arnaud Charleroy
// */
//@Api("/log")
//@Path("/log")
//public class LogResourceService {
//
//    /**
//     * Returns the logs create by the search of data from environment services or data services
//     * @param pageSize
//     * @param page
//     * @param userUri
//     * @param startDate
//     * @param endDate
//     * @param dateSortAsc
//     * @return The file content or null with a 404 status if it doesn't exists
//     */
//    @GET
//    @Path("querylog")
//    @ApiOperation(value = "Get data query logs")
//    @ApiResponses(value = {
//        @ApiResponse(code = 200, message = "Retrieve data query logs", response = DataQueryLog.class, responseContainer = "List"),
//        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
//        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
//        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
//    })
//    @ApiProtected
//    @Produces(MediaType.APPLICATION_JSON)  
//    public Response getDataQueryLogSearch(
//        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam(GlobalWebserviceValues.PAGE_SIZE) @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) @Min(0) int pageSize,
//        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam(GlobalWebserviceValues.PAGE) @DefaultValue(DefaultBrapiPaginationValues.PAGE) @Min(0) int page,
//        @ApiParam(value = "Search by user uri", example = DocumentationAnnotation.EXAMPLE_VARIABLE_URI) @QueryParam("userUri") @URL String userUri,
//        @ApiParam(value = "Search by minimal date", example = DocumentationAnnotation.EXAMPLE_XSDDATETIME) @QueryParam("startDate") @Date({DateFormat.YMDTHMSZ, DateFormat.YMD}) String startDate,
//        @ApiParam(value = "Search by maximal date", example = DocumentationAnnotation.EXAMPLE_XSDDATETIME) @QueryParam("endDate") @Date({DateFormat.YMDTHMSZ, DateFormat.YMD}) String endDate,
//        @ApiParam(value = "Date search result order ('true' for ascending and 'false' for descending)", example = "true") @QueryParam("dateSortAsc") boolean dateSortAsc
//    ) {
//        
//   
//        DataQueryLogDAO dataDAO = new DataQueryLogDAO();
//         
//    
//    }
//    
////    private DataLogAccessUserDTO lookupUser(List<User> personList, String userUri) {
////        User foundUser = personList.stream().
////        filter(p -> p.getUri() != null && p.getUri().equals(userUri)).
////        findAny().orElse(null);
////        if(foundUser != null){
////            DataLogAccessUserDTO returnedUser = new DataLogAccessUserDTO();
////            returnedUser.setUri(foundUser.getUri());
////            returnedUser.setFirstName(foundUser.getFamilyName());
////            returnedUser.setFamilyName(foundUser.getFirstName());
////            return returnedUser;
////        }
////        return null;
////    }
//}