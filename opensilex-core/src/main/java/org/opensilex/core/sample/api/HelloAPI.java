//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.sample.api;

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
import org.opensilex.server.security.ApiProtected;
import org.opensilex.server.security.dal.UserModel;



/**
 * Sample webservice which just return "Hello World !" on any request
 */
@Api("Hello")
@Path("/hello")
public class HelloAPI {

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
     * @param securityContext Jersey Security Context // TODO add link
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

        UserModel user = (UserModel) securityContext.getUserPrincipal();

        return "Hello: " + user.getName();
    }

}
