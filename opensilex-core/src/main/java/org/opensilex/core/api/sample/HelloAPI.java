//******************************************************************************
//                           HelloWorldAPI.java
// OpenSILEX
// Copyright © INRA 2019
// Creation date: 01 jan. 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.api.sample;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import org.opensilex.server.security.user.User;
import org.opensilex.server.security.ApiProtected;
import org.opensilex.server.rest.RestApplicationAPI;

/**
 * Sample webservice which just return "Hello World !" on any request
 */
@Api("Hello")
@Path("/hello")
public class HelloAPI implements RestApplicationAPI {

    /**
     * Hello world public webservice
     *
     * @return "Hello World !"
     */
    @GET
    @Path("/world")
    @ApiOperation(value = "Say hello to the world !")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Hello message"),
        @ApiResponse(code = 500, message = "Internal error")
    })
    @Produces(MediaType.TEXT_PLAIN)
    public String world() {
        return "Hello World !!";
    }

    public boolean isProdEnabled() {
        return false;
    }

    /**
     * Hello to a user defined with his token
     *
     * @return "Hello World !"
     */
    @GET
    @Path("/user")
    @ApiOperation(value = "Say hello to the user !")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Hello message"),
        @ApiResponse(code = 500, message = "Internal error")
    })
    @ApiProtected
    @Produces(MediaType.TEXT_PLAIN)
    public String world(@Context SecurityContext securityContext) {

        User user = (User) securityContext.getUserPrincipal();

        return "Hello: " + user.getName();
    }

}
