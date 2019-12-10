/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.api;

import com.google.common.hash.Hashing;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Pattern;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.apache.http.HttpStatus;
import org.opensilex.OpenSilex;
import org.opensilex.front.FrontModule;
import org.opensilex.module.OpenSilexModule;
import org.opensilex.server.rest.RestApplicationAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opensilex.front.FrontConfig;
import org.opensilex.server.exceptions.NotFoundException;

/**
 * Service to produce angular application configuration
 */
@Api("Front")
@Path("/front")
public class FrontAPI implements RestApplicationAPI {

    private final static Logger LOGGER = LoggerFactory.getLogger(FrontAPI.class);

    @Inject
    OpenSilex app;

    @Inject
    FrontModule frontModule;

    @Context
    HttpServletRequest request;

    @GET
    @Path("/config")
    @ApiOperation(value = "Return the current configuration", response = FrontConfigDTO.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Plugin configuration"),
        @ApiResponse(code = 500, message = "Internal error")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfig() throws Exception {

        FrontConfigDTO config = frontModule.getConfigDTO();
        
        return Response.ok().entity(config).build();
    }

    @GET
    @Path("/extension/{module}.js")
    @ApiOperation(value = "Return the front Vue JS extension file to include")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return the extension")
    })
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getExtension(
            @PathParam("module") @Pattern(regexp = "([a-zA-Z0-9-]+$)") String moduleId,
            @Context Request request
    ) throws Exception {

        List<OpenSilexModule> modules = app.getModulesByProjectId(moduleId);

        if (modules.size() > 0) {
            OpenSilexModule module = modules.get(0);

            String filePath = getModuleFrontLibFilePath(moduleId);

            if (module.fileExists(filePath)) {

                EntityTag etag = new EntityTag(Hashing.sha256().hashUnencodedChars(module.getClass().getName() + "_" + getModuleFrontLibFileName(moduleId) + "_" + module.getLastModified(filePath).toString()).toString());

                ResponseBuilder builder = request.evaluatePreconditions(etag);

                CacheControl cc = new CacheControl();
                cc.setPrivate(true);
                cc.setNoTransform(true);
                cc.setMaxAge(0);
                cc.setMustRevalidate(true);

                if (builder == null) {
                    return Response
                            .ok(module.getFileInputStream(filePath), "application/javascript")
                            .cacheControl(cc)
                            .tag(etag)
                            .header("Content-Disposition", "attachment; filename=\"" + getModuleFrontLibFileName(moduleId) + "\"")
                            .build();
                } else {
                    return builder
                            .status(HttpStatus.SC_NOT_MODIFIED)
                            .cacheControl(cc)
                            .tag(etag)
                            .build();
                }

            }
        }

        throw new NotFoundException("No Vue JS extension found for module: " + moduleId);
    }

    public static String getModuleFrontLibFilePath(String moduleId) {
        return FrontModule.FRONT_EXTENSIONS_DIRECTORY + getModuleFrontLibFileName(moduleId);
    }

    public static String getModuleFrontLibFileName(String moduleId) {
        return moduleId + ".umd.min.js";
    }

}
