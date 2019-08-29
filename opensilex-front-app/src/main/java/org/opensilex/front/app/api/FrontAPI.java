/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.app.api;

import com.google.common.reflect.ClassPath;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.opensilex.OpenSilex;
import org.opensilex.front.app.FrontAppExtension;
import org.opensilex.module.OpenSilexModule;
import org.opensilex.server.rest.RestApplicationAPI;
import org.opensilex.utils.ClassInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to produce angular application configuration
 */
@Api("Front")
@Path("/front")
public class FrontAPI implements RestApplicationAPI {

    private final static Logger LOGGER = LoggerFactory.getLogger(FrontAPI.class);

    @Inject
    OpenSilex app;

    @Context
    UriInfo uri;
    
    @GET
    @Path("/config")
    @ApiOperation(value = "Return the current configuration")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Plugin configuration"),
        @ApiResponse(code = 500, message = "Internal error")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response config() throws Exception {

        Map<String, PluginConfigDTO> result = new HashMap<>();
        
        for (FrontAppExtension frontAppExtension : app.getModulesImplementingInterface(FrontAppExtension.class)) {
            OpenSilexModule module = (OpenSilexModule)frontAppExtension;
            for (String pluginFileName : module.listResourceDirectory("angular/plugins")) {
                if (pluginFileName.endsWith(".js")) {
                    String pluginName = pluginFileName.substring(0, pluginFileName.length() - 3);

                    PluginConfigDTO pluginConfigDTO = new PluginConfigDTO();
                    pluginConfigDTO.setName(pluginName);

                    pluginConfigDTO.setPath(uri.getBaseUri().toString() + "front/plugin/" + ClassInfo.getProjectIdFromClass(frontAppExtension.getClass()) + "/" + pluginName + ".js");

                    if (!pluginName.equals("shared")) {
                        pluginConfigDTO.getDeps().add("shared");
                    }

                    result.put(pluginName, pluginConfigDTO);
                }
            }
        }

        return Response.ok().entity(result).build();
    }

    @GET
    @Path("/plugin/{module}/{plugin}.js")
    @ApiOperation(value = "Return the front plugin sources")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Plugin source"),
        @ApiResponse(code = 500, message = "Internal error")
    })
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response pluginByName(
            @PathParam("module") String moduleId,
            @PathParam("plugin") String pluginId
    ) throws Exception {

        List<OpenSilexModule> modules = app.getModulesByProjectId(moduleId);

        if (modules.size() > 0) {
            OpenSilexModule module = modules.get(0);

            String fileName = pluginId + ".js";
            String filePath = "angular/plugins/" + fileName;
            if (module.fileExists(filePath)) {
                return Response
                        .ok(module.getFileInputStream(filePath), "application/javascript")
                        .header("content-disposition", "attachment; filename = " + fileName)
                        .build();
            }
        }

        // TODO return not found
        return null;
    }
}
