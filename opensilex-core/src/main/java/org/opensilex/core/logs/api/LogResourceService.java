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
//import javax.ws.rs.GET;
//import javax.ws.rs.Path;
//import javax.ws.rs.Produces;
//import javax.ws.rs.QueryParam;
//import javax.ws.rs.core.MediaType;
//import javax.ws.rs.core.Response;
//import org.opensilex.core.logs.filters.DataQueryLogFilter;
// 
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
//        @ApiResponse(code = 200, message = "Retrieve data query logs", response = LogDTO.class, responseContainer = "List"),
//        
//    })
//    @ApiProtected
//    @Produces(MediaType.APPLICATION_JSON)  
//    public Response getDataQueryLogSearch(
////            @ApiParam(value = "Search by user uri") @QueryParam("userUri") @URL String userUri,
////        @ApiParam(value = "Search by minimal date") @QueryParam("startDate") @Date({DateFormat.YMDTHMSZ, DateFormat.YMD}) String startDate,
////        @ApiParam(value = "Search by maximal date") @QueryParam("endDate") @Date({DateFormat.YMDTHMSZ, DateFormat.YMD}) String endDate,
////        @ApiParam(value = "Date search result order ('true' for ascending and 'false' for descending)", example = "true") @QueryParam("dateSortAsc") boolean dateSortAsc
//    ) {
//        
//    return 
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