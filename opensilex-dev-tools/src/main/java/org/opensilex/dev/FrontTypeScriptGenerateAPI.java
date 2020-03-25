//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.dev;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.Swagger;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import opensilex.service.PhisWsModule;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModule;
import static org.opensilex.dev.StartServerWithFront.isWindows;
import org.opensilex.fs.local.LocalFileSystemConnection;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.utils.ClassUtils;
import org.opensilex.utils.SwaggerAPIGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vincent
 */
public class FrontTypeScriptGenerateAPI {
    
    private static String nodeBin = "node";
    
    private final static Logger LOGGER = LoggerFactory.getLogger(FrontTypeScriptGenerateAPI.class);
    
    public static void main(String[] args) throws Exception {
        FrontTypeScriptGenerateAPI.generate(null);
    }
    
    private static OpenSilex opensilex;
    
    public static void generate(String baseDirectory) throws Exception {
        
        Map<String, String> args = new HashMap<String, String>() {
            {
                put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.DEV_PROFILE_ID);
//                put(OpenSilex.DEBUG_ARG_KEY, "true");
            }
        };
        
        if (baseDirectory != null) {
            args.put(OpenSilex.BASE_DIR_ARG_KEY, baseDirectory);
            args.put(OpenSilex.CONFIG_FILE_ARG_KEY, getConfig(baseDirectory));
        } else {
            args.put(OpenSilex.CONFIG_FILE_ARG_KEY, getConfig(System.getProperty("user.dir")));
        }
        
        OpenSilex.setup(args);
        
        opensilex = OpenSilex.getInstance();
        
        if (isWindows()) {
            nodeBin += ".exe";
        }
        
        for (OpenSilexModule module : opensilex.getModules()) {
            if (ClassUtils.isJarClassDirectory(module.getClass())) {
                Swagger moduleAPI = SwaggerAPIGenerator.getModuleApi(module.getClass());
                if (moduleAPI != null) {
                    ObjectMapper mapper = new ObjectMapper();
                    String jsonInString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(moduleAPI);
                    File targetDirectory = ClassUtils.getJarFile(module.getClass());
                    Path modulePath = Paths.get(targetDirectory.getAbsolutePath()).resolve("../..");
                    if (module.getClass().equals(PhisWsModule.class)) {
                        modulePath = modulePath.resolve("../");
                    }
                    Path swaggerJsonLibPath = modulePath.resolve("front/src/lib/");
                    File swaggerJsonLibDir = swaggerJsonLibPath.toFile();
                    FileStorageService fs = LocalFileSystemConnection.getService(swaggerJsonLibPath);
                    if (!swaggerJsonLibDir.exists()) {
                        fs.createDirectories(Paths.get("/"));
                    }
                    
                    Path swaggerPath = Paths.get(swaggerJsonLibPath.resolve("swagger.json").toFile().getCanonicalPath());
                    LOGGER.info("Write swagger definition to: " + swaggerPath);
                    LOGGER.debug(jsonInString);
                    fs.writeFile(swaggerPath, jsonInString);
                    
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
