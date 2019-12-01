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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
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
import org.opensilex.utils.ClassInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opensilex.config.ConfigManager;
import org.opensilex.front.FrontConfig;
import org.opensilex.front.FrontExtension;
import org.opensilex.front.FrontPluginConfig;
import org.opensilex.front.FrontPluginMenuConfig;

/**
 * Service to produce angular application configuration
 */
@Api("Angular")
@Path("/angular")
public class FrontAPI implements RestApplicationAPI {

    private final static Logger LOGGER = LoggerFactory.getLogger(FrontAPI.class);

    private final static String ANGULAR_PLUGINS_DIRECTORY = "angular/plugins/";
    @Inject
    OpenSilex app;

    @Inject
    FrontModule angularModule;

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
    public Response config() throws Exception {

        FrontConfigDTO config = new FrontConfigDTO();

        FrontConfig angularConfig = angularModule.getConfig(FrontConfig.class);
        config.setWelcomeComponent(angularConfig.welcomeComponent());
        config.setHomeComponent(angularConfig.homeComponent());
        config.setNotFoundComponent(angularConfig.notFoundComponent());
        config.setHeaderComponent(angularConfig.headerComponent());
        config.setLoginComponent(angularConfig.loginComponent());
        config.setMenuComponent(angularConfig.menuComponent());
        config.setFooterComponent(angularConfig.footerComponent());

        config.setPlugins(getPluginsConfig());

        Map<String, String> menu = new HashMap<>();
        pluginsConfig.forEach((String pluginName, FrontPluginConfig cfg) -> {
            for (FrontPluginMenuConfig menuEntry : cfg.menu()) {
                menu.put(menuEntry.path(), menuEntry.label());
            }
        });
        config.setMenu(menu);

        return Response.ok().entity(config).build();

    }

    private Map<String, FrontPluginConfig> pluginsConfig;

    private Map<String, FrontPluginConfigDTO> getPluginsConfig() throws Exception {
        Map<String, FrontPluginConfigDTO> result = new HashMap<>();

        ConfigManager cfgManager = new ConfigManager();
        pluginsConfig = new HashMap<>();

        for (FrontExtension angularExtension : app.getModulesImplementingInterface(FrontExtension.class)) {
            OpenSilexModule module = (OpenSilexModule) angularExtension;

            if (module.fileExists("opensilex.ng.yml")) {
                cfgManager.addSource(module.getFileInputStream("opensilex.ng.yml"));
            }

            for (String pluginFileName : module.listResourceDirectory("angular/plugins")) {
                if (pluginFileName.endsWith(".js")) {
                    String pluginName = pluginFileName.substring(0, pluginFileName.length() - 3);

                    FrontPluginConfig cfg = cfgManager.loadConfig(pluginName, FrontPluginConfig.class);
                    pluginsConfig.put(pluginName, cfg);

                    FrontPluginConfigDTO pluginConfigDTO = new FrontPluginConfigDTO();
                    pluginConfigDTO.setName(pluginName);

                    String path = ClassInfo.getProjectIdFromClass(angularExtension.getClass());
                    if (!path.isEmpty()) {
                        long modifiedTime = module.getLastModified(ANGULAR_PLUGINS_DIRECTORY + pluginName + ".js").getTime();
                        pluginConfigDTO.setPath(getRestAPI() + "angular/plugin/" + ClassInfo.getProjectIdFromClass(angularExtension.getClass()) + "/" + pluginName + ".js?" + modifiedTime);

                        if (!pluginName.equals("shared")) {
                            pluginConfigDTO.getDeps().add("shared");
                        }
                        
                        pluginConfigDTO.getDeps().addAll(cfg.dependencies());

                        result.put(pluginName, pluginConfigDTO);
                    }
                }
            }
        }
        return result;
    }

    @GET
    @Path("/plugin/{module}/{plugin}.js")
    @ApiOperation(value = "Return the angular plugin sources")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Plugin source"),
        @ApiResponse(code = 500, message = "Internal error")
    })
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response pluginByName(
            @PathParam("module") String moduleId,
            @PathParam("plugin") String pluginId,
            @Context Request request
    ) throws Exception {

        List<OpenSilexModule> modules = app.getModulesByProjectId(moduleId);

        if (modules.size() > 0) {
            OpenSilexModule module = modules.get(0);

            String fileName = pluginId + ".js";
            String filePath = ANGULAR_PLUGINS_DIRECTORY + fileName;
            if (module.fileExists(filePath)) {

                EntityTag etag = new EntityTag(Hashing.sha256().hashUnencodedChars(module.getClass().getName() + "_" + fileName + "_" + module.getLastModified(filePath).toString()).toString());

                ResponseBuilder builder = request.evaluatePreconditions(etag);

                CacheControl cc = new CacheControl();
                cc.setPrivate(true);
                cc.setNoTransform(true);
                cc.setMaxAge(31536000);

                if (builder == null) {
                    return Response
                            .ok(module.getFileInputStream(filePath), "application/javascript")
                            .cacheControl(cc)
                            .tag(etag)
                            .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
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

        // TODO return not found
        return null;
    }

    private String getRestAPI() {
        String scheme = request.getScheme();
        String proxyScheme = request.getHeader("x-forwarded-proto");
        if (proxyScheme != null && !proxyScheme.equals(scheme)) {
            scheme = proxyScheme;
        }

        String host = request.getHeader("host");

        return scheme + "://" + host + "/rest/";
    }
}
