//******************************************************************************
//                       APIResourceService.java
// SILEX-PHIS
// Copyright © INRA
// Creation date: 25 Septembre 2019
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.dto.APIDescriptionDTO;

/**
 * Api ressource service
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
@Api("/api")
@Path("/api")
public class APIResourceService extends ResourceService {

    /**
     * Describe API state
     * @return list of API descritpions
     * @example
     * {
     *   "majorVersion": 3,
     *   "version": "v3.3.0",
     *   "name": "OpenSILEX API",
     *   "build": null
     * }
     */
    @GET
    @ApiOperation(value = "Retrieve informations about OpenSILEX API",
                  notes = "Retreive version, name and major version number.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve API description", response = APIDescriptionDTO.class),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @Produces(MediaType.APPLICATION_JSON)  
    public Response getApiDescription() {
        APIDescriptionDTO apiDescription = new APIDescriptionDTO();
        return Response.status(Response.Status.OK).entity(apiDescription).build();
    }
}