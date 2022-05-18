//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.dev;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.codegen.ClientOptInput;
import io.swagger.codegen.DefaultGenerator;
import io.swagger.codegen.config.CodegenConfigurator;
import io.swagger.models.Swagger;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModule;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.utils.ClassUtils;
import org.opensilex.utils.SwaggerAPIGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
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
                Swagger moduleAPI = SwaggerAPIGenerator.getModuleApi(module.getClass(), opensilex.getReflections());
                if (moduleAPI != null) {
                    LOGGER.info("Process Module API: " + module.getClass().getCanonicalName());
                    moduleAPI.setHost("localhost");
                    File targetDirectory = ClassUtils.getJarFile(module.getClass());
                    Path modulePath = Paths.get(targetDirectory.getAbsolutePath()).resolve("../..");
                    if (module.getClass().getCanonicalName().equals("opensilex.service.PhisWsModule")) {
                        modulePath = modulePath.resolve("../");
                    }
                    Path swaggerJsonLibPath = modulePath.resolve("front/src/lib/");
                    Path swaggerPath = Paths.get(swaggerJsonLibPath.resolve("swagger.json").toFile().getCanonicalPath());

                    FileUtils.deleteDirectory(swaggerJsonLibPath.toFile());
                    FileUtils.writeStringToFile(swaggerJsonLibPath.resolve(".gitkeep").toFile(), "", StandardCharsets.UTF_8);

                    ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
                    String jsonInString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(moduleAPI);
                    LOGGER.info("Write swagger definition to: " + swaggerPath);
                    LOGGER.debug(jsonInString);
                    FileUtils.writeStringToFile(swaggerPath.toFile(), jsonInString, StandardCharsets.UTF_8);

                    String moduleID = ClassUtils.getProjectIdFromClass(module.getClass());
                    LOGGER.info("Build TS library: " + moduleID);
                    CodegenConfigurator configurator = new CodegenConfigurator();
                    configurator.setInputSpec(swaggerPath.toString());
                    configurator.setTemplateDir(modulePath.resolve("../opensilex-main/src/main/resources/swagger/templates/typescript-inversify").toFile().getCanonicalPath());
                    configurator.setLang("typescript-inversify");
                    configurator.setOutputDir(swaggerJsonLibPath.toString());
                    configurator.addAdditionalProperty("packageName", moduleID);
                    configurator.addAdditionalProperty("packageVersion", "SNAPSHOT");
                    configurator.addAdditionalProperty("npmName", moduleID);
                    configurator.addAdditionalProperty("usePromise", true);
                    configurator.addAdditionalProperty("useHttpClient", true);
                    configurator.addAdditionalProperty("modelPropertyNaming", "original");
                    ClientOptInput opts = configurator.toClientOptInput();
                    opts.setSwagger(moduleAPI);

                    DefaultGenerator codeGen = new DefaultGenerator();
                    codeGen.opts(opts).generate();

                    LOGGER.info("Build TS types: " + swaggerPath);
                    Process process = createFrontTypes(modulePath, swaggerJsonLibPath);
                    process.waitFor();
                }
            }
        }
    }

    private static Process createFrontTypes(Path baseDirectory, Path libDirectory) throws Exception {
        List<String> args = new ArrayList<>();
        args.add(baseDirectory.resolve("../.node/node/" + nodeBin).toFile().getCanonicalPath());
        args.add(baseDirectory.resolve("../.node/node/yarn/dist/bin/yarn.js").toFile().getCanonicalPath());
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
