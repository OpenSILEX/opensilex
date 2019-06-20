//******************************************************************************
//                           HelloWorldAPI.java
// OpenSILEX
// Copyright © INRA 2019
// Creation date: 01 jan. 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.module.core.api.sample;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.opensilex.server.rest.RestApplicationAPI;

/**
 * Sample webservice which just return "Hello World !" on any request
 */
@Api("/hello")
@Path("/hello")
public class HelloWorldAPI implements RestApplicationAPI {

    /**
     * Hello world webservice configured with token
     * 
     * @return "Hello World !"
     */
    @GET
    @ApiOperation(value = "Say hello !")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Hello message"),
        @ApiResponse(code = 500, message = "Internal error")
    })
    @ApiImplicitParams({
        @ApiImplicitParam(name = "Authorization", required = true,
                dataType = "string", paramType = "header",
                value = "Access token given",
                example = "Bearer ")
    })
    @Produces(MediaType.TEXT_PLAIN)
    public String sayHello() {
        return "Hello World !";
    }
    
    public boolean isProdEnabled() {
        return false;
    }
}