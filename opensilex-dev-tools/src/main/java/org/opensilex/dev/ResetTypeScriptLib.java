//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.dev;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModule;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.utils.ClassUtils;
import org.opensilex.utils.SwaggerAPIGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to regenerate OpenAPI specification and TypeScript client library.
 *
 * @author vincent
 */
public class ResetTypeScriptLib {

    private static String nodeBin = "node"; 

    private final static Logger LOGGER = LoggerFactory.getLogger(ResetTypeScriptLib.class);

    public static void main(String[] args) throws Exception {
        ResetTypeScriptLib.generate(null);
    }

    private static OpenSilex opensilex;

    public static void generate(Path baseDirectory) throws Exception {

        Map<String, String> customArgs = new HashMap<>();
        customArgs.put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.INTERNAL_OPERATIONS_PROFILE_ID);

        opensilex = DevModule.getOpenSilexDev(baseDirectory,
                customArgs);

        if (DevModule.isWindows()) {
            nodeBin += ".exe"; 
        }

        for (OpenSilexModule module : opensilex.getModules()) {
            if (ClassUtils.isJarClassDirectory(module.getClass())) {
                OpenAPI moduleAPI = SwaggerAPIGenerator.getModuleApi(module.getClass(), opensilex.getReflections());
                if (moduleAPI != null) {
                    LOGGER.info("Process Module API: " + module.getClass().getCanonicalName());
                    File targetDirectory = ClassUtils.getJarFile(module.getClass());
                    Path modulePath = Paths.get(targetDirectory.getAbsolutePath()).resolve("../..");
                    if (module.getClass().getCanonicalName().equals("opensilex.service.PhisWsModule")) {
                        modulePath = modulePath.resolve("../");
                    }
                    Path swaggerJsonLibPath = modulePath.resolve("front/src/lib/");
                    Path openApiJsonPath = Paths.get(swaggerJsonLibPath.resolve("openapi.json").toFile().getCanonicalPath());

                    FileUtils.deleteDirectory(swaggerJsonLibPath.toFile());
                    FileUtils.writeStringToFile(swaggerJsonLibPath.resolve(".gitkeep").toFile(), "", StandardCharsets.UTF_8);

                    ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
                    String jsonInString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(moduleAPI);
                    LOGGER.info("Write openapi definition to: " + openApiJsonPath);
                    LOGGER.debug(jsonInString);
                    FileUtils.writeStringToFile(openApiJsonPath.toFile(), jsonInString, StandardCharsets.UTF_8);

                    String moduleID = ClassUtils.getProjectIdFromClass(module.getClass());
                    LOGGER.info("Build TS library with Hey API: " + moduleID);

                    Path nodeBinPath = modulePath.resolve("../node_modules/.bin/openapi-ts");
                    if (!nodeBinPath.toFile().exists()) {
                        nodeBinPath = modulePath.resolve("../../node_modules/.bin/openapi-ts");
                    }

                    List<String> openApiTsArgs = new ArrayList<>();
                    openApiTsArgs.add(nodeBinPath.toFile().getCanonicalPath());
                    openApiTsArgs.add("--plugins");
                    openApiTsArgs.add("@hey-api/client-fetch");
                    openApiTsArgs.add("--plugins");
                    openApiTsArgs.add("@hey-api/typescript");
                    openApiTsArgs.add("--plugins");
                    openApiTsArgs.add("@hey-api/sdk");
                    openApiTsArgs.add("-i");
                    openApiTsArgs.add(openApiJsonPath.toString());
                    openApiTsArgs.add("-o");
                    openApiTsArgs.add(swaggerJsonLibPath.resolve("generated").toString());

                    ProcessBuilder openApiTsBuilder = new ProcessBuilder(openApiTsArgs);
                    openApiTsBuilder.directory(modulePath.toFile());
                    openApiTsBuilder.inheritIO();
                    Process openApiTsProcess = openApiTsBuilder.start();
                    int exitCode = openApiTsProcess.waitFor();
                    if (exitCode != 0) {
                        LOGGER.warn("openapi-ts execution returned non-zero exit code: " + exitCode);
                    }
                }
            }
        }
    }

    private static Process createFrontTypes(Path baseDirectory, Path libDirectory) throws Exception {
        List<String> args = new ArrayList<>();
        args.add(baseDirectory.resolve("../.node/node/" + nodeBin).toFile().getCanonicalPath());
        args.add(baseDirectory.resolve("../.node/node/node_modules/npm/bin/npm-cli.js").toFile().getCanonicalPath());
        args.add("run");
        args.add("build:types");
        ProcessBuilder typeBuilder = new ProcessBuilder(args);
        typeBuilder.directory(libDirectory.toFile());
        typeBuilder.inheritIO();

        return typeBuilder.start();
    }

    private static String getConfig(String baseDirectory) {
        return Paths.get(baseDirectory).resolve(DevModule.CONFIG_FILE_PATH).toFile().getAbsolutePath();
    }
}
