/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.api;

import com.google.common.hash.Hashing;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import javax.inject.Inject;
import javax.validation.constraints.Pattern;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
import org.opensilex.OpenSilexModule;
import org.opensilex.config.ConfigManager;
import org.opensilex.front.theme.ThemeBuilder;
import org.opensilex.front.theme.ThemeConfig;
import org.opensilex.security.authentication.ApiTranslatable;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.service.SPARQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opensilex.server.exceptions.NotFoundException;
import org.opensilex.server.response.SingleObjectResponse;

/**
 * Service to produce angular application configuration
 */
@Api("Vue.js")
@Path("/vuejs")
public class FrontAPI {

    private final static Logger LOGGER = LoggerFactory.getLogger(FrontAPI.class);

    @Inject
    private OpenSilex app;

    @Inject
    private FrontModule frontModule;

    @Inject
    private SPARQLService sparql;

    @CurrentUser
    AccountModel user;
        
    @GET
    @Path("/config")
    @ApiOperation(value = "Return the current configuration")
    @ApiTranslatable
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Front application configuration", response = FrontConfigDTO.class)
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfig() throws Exception {

        FrontConfigDTO config = frontModule.getConfigDTO(user, sparql);

        return new SingleObjectResponse<FrontConfigDTO>(config).getResponse();
    }

    @GET
    @Path("/user_config")
    @ApiOperation(value = "Return the user-specific configuration")
    @ApiTranslatable
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Front application configuration", response = UserFrontConfigDTO.class)
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserConfig() throws Exception {
        UserFrontConfigDTO config = frontModule.getUserConfigDTO(user);

        return new SingleObjectResponse<>(config).getResponse();
    }

    @GET
    @Path("/extension/js/{module}.js")
    @ApiOperation(value = "Return the front Vue JS extension file to include")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return the extension file", response = File.class)
    })
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getExtension(
            @PathParam("module") @ApiParam(value = "Module identifier", example = "opensilex") @Pattern(regexp = "([a-zA-Z0-9-]+$)") String moduleId,
            @Context Request request
    ) throws Exception {

        OpenSilexModule module = getModule(moduleId);

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

        throw new NotFoundException("No Vue JS extension found for module: " + moduleId);
    }

    @GET
    @Path("/extension/css/{module}.css")
    @ApiOperation(value = "Return the front Vue JS extension css file to include")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return the extension css file", response = File.class)
    })
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getExtensionStyle(
            @PathParam("module") @ApiParam(value = "Module identifier", example = "opensilex") @Pattern(regexp = "([a-zA-Z0-9-]+$)") String moduleId,
            @Context Request request
    ) throws Exception {

        OpenSilexModule module = getModule(moduleId);

        String filePath = getModuleFrontLibCssFilePath(moduleId);

        EntityTag etag = new EntityTag(Hashing.sha256().hashUnencodedChars(module.getClass().getName() + "_" + getModuleFrontLibCssFileName(moduleId) + "_" + module.getLastModified(filePath).toString()).toString());

        ResponseBuilder builder = request.evaluatePreconditions(etag);

        CacheControl cc = new CacheControl();
        cc.setPrivate(true);
        cc.setNoTransform(true);
        cc.setMaxAge(0);
        cc.setMustRevalidate(true);

        if (builder == null) {
            if (module.fileExists(filePath)) {
                return Response
                        .ok(module.getFileInputStream(filePath), "text/css")
                        .cacheControl(cc)
                        .tag(etag)
                        .header("Content-Disposition", "attachment; filename=\"" + getModuleFrontLibCssFileName(moduleId) + "\"")
                        .build();
            } else {
                return Response
                        .ok("", "text/css")
                        .cacheControl(cc)
                        .tag(etag)
                        .header("Content-Disposition", "attachment; filename=\"" + getModuleFrontLibCssFileName(moduleId) + "\"")
                        .build();
            }
        } else {
            return builder
                    .status(HttpStatus.SC_NOT_MODIFIED)
                    .cacheControl(cc)
                    .tag(etag)
                    .build();
        }
    }

    @GET
    @Path("/theme/{moduleId}/{themeId}/config")
    @ApiOperation(value = "Return the front Vue JS theme configuration")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return the theme configuration", response = ThemeConfigDTO.class)
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getThemeConfig(
            @PathParam("moduleId") @ApiParam(value = "Module identifier", example = "opensilex-front") @Pattern(regexp = "([a-zA-Z0-9-]+$)") String moduleId,
            @PathParam("themeId") @ApiParam(value = "Theme identifier", example = "phis") @Pattern(regexp = "([a-zA-Z0-9-]+$)") String themeId
    ) throws Exception {

        OpenSilexModule module = getModule(moduleId);

        ThemeConfig config = getThemeConfigOrNull(module, themeId);

        if (config != null) {
            ThemeConfigDTO themeConfig = ThemeConfigDTO.fromThemeConfig(config);
            return new SingleObjectResponse<ThemeConfigDTO>(themeConfig).getResponse();

        }

        throw new NotFoundException("No theme configuration found for module: " + moduleId + " - with name: " + themeId);
    }

    @GET
    @Path("/theme/{moduleId}/{themeId}/style.css")
    @ApiOperation(value = "Return the theme css file")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return the theme css file", response = File.class)
    })
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getThemeCss(
            @PathParam("moduleId") @ApiParam(value = "Module identifier", example = "opensilex-front") @Pattern(regexp = "([a-zA-Z0-9-]+$)") String moduleId,
            @PathParam("themeId") @ApiParam(value = "Theme identifier", example = "phis") @Pattern(regexp = "([a-zA-Z0-9-]+$)") String themeId
    ) throws Exception {

        OpenSilexModule module = getModule(moduleId);

        ThemeConfig config = getThemeConfigOrNull(module, themeId);

        if (config != null) {

            ThemeBuilder themeBuilder = getThemeBuilder(module, themeId, config);
            String css = themeBuilder.buildCss();

            // Load file
            return Response
                    .ok(css, "text/css")
                    .header("Content-Disposition", "attachment; filename=\"" + "style.css" + "\"")
                    .build();
        }

        return Response
                .ok("", "text/css")
                .header("Content-Disposition", "attachment; filename=\"" + "style.css" + "\"")
                .build();
    }

    private ThemeBuilder getThemeBuilder(OpenSilexModule module, String themeId, ThemeConfig config) throws Exception {
        ThemeBuilder parentThemeBuilder = null;
        if (config.extend() != null && !config.extend().isEmpty()) {
            String[] extendedTheme = config.extend().split("#");
            if (extendedTheme.length != 2) {
                LOGGER.error("Invalid extension theme ID: " + config.extend() + " for theme: " + themeId);
            } else {
                OpenSilexModule parentThemeModule = getModule(extendedTheme[0]);

                ThemeConfig parentConfig = getThemeConfigOrNull(parentThemeModule, extendedTheme[1]);

                parentThemeBuilder = getThemeBuilder(parentThemeModule, extendedTheme[1], parentConfig);
            }
        }

        ThemeBuilder themeBuilder = new ThemeBuilder(module, themeId, config, parentThemeBuilder);

        return themeBuilder;
    }

    @GET
    @Path("/theme/{moduleId}/{themeId}/resource")
    @ApiOperation(value = "Return the theme requested resource")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return the resource", response = File.class)
    })
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getThemeResource(
            @PathParam("moduleId") @ApiParam(value = "Module identifier", example = "opensilex-front") @Pattern(regexp = "([a-zA-Z0-9-]+$)") String moduleId,
            @PathParam("themeId") @ApiParam(value = "Theme identifier", example = "phis") @Pattern(regexp = "([a-zA-Z0-9-]+$)") String themeId,
            @ApiParam(value = "Resource path", example = "images/opensilex.png") @QueryParam("filePath") String filePath
    ) throws Exception {

        OpenSilexModule module = getModule(moduleId);

        String themeFilePath = getModuleFrontThemeResourcePath(themeId, filePath);
        String fileName = getFileName(filePath);

        if (module.fileExists(themeFilePath)) {
            String mimeType = module.getFileMimeType(themeFilePath);

            // Load file
            return Response
                    .ok(module.getFileInputStream(themeFilePath), mimeType)
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .build();
        } else {
            throw new NotFoundException("Resource file not found for module: " + moduleId + " - with theme: " + themeId + " - with path: " + filePath);
        }
    }

    public static String getModuleFrontLibFilePath(String moduleId) {
        return FrontModule.FRONT_EXTENSIONS_DIRECTORY + getModuleFrontLibFileName(moduleId);
    }

    private static String getModuleFrontLibFileName(String moduleId) {
        return moduleId + ".umd.min.js";
    }

    public static String getModuleFrontLibCssFilePath(String moduleId) {
        return FrontModule.FRONT_EXTENSIONS_DIRECTORY + getModuleFrontLibCssFileName(moduleId);
    }

    private static String getModuleFrontLibCssFileName(String moduleId) {
        return moduleId + ".css";
    }

    private static String getModuleFrontThemePath(String themeId) {
        return FrontModule.FRONT_EXTENSIONS_DIRECTORY + "theme/" + themeId + "/" + themeId + ".yml";
    }

    private static String getModuleFrontThemeResourcePath(String themeId, String filePath) {
        return FrontModule.FRONT_EXTENSIONS_DIRECTORY + "theme/" + themeId + "/" + filePath;
    }

    private static String getFileName(String filePath) {
        return Paths.get(filePath).getFileName().toString();
    }

    private OpenSilexModule getModule(String moduleId) {
        List<OpenSilexModule> modules = app.getModulesByProjectId(moduleId);

        if (modules.size() > 0) {
            return modules.get(0);
        }

        throw new NotFoundException("Module not found: " + moduleId);
    }

    private ThemeConfig getThemeConfigOrNull(OpenSilexModule module, String themeId) throws IOException, URISyntaxException {

        String filePath = getModuleFrontThemePath(themeId);

        if (module.fileExists(filePath)) {

            ConfigManager cfg = new ConfigManager();
            cfg.addSource(module.getFileInputStream(filePath));
            return cfg.loadConfig("", ThemeConfig.class);

        }

        return null;
    }

}
