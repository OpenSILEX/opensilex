package org.opensilex.utils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.annotations.Api;
import io.swagger.jaxrs.Reader;
import io.swagger.jaxrs.config.SwaggerContextService;
import io.swagger.models.Swagger;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModule;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

/**
 * Helper class to generate Swagger API JSON file.
 *
 * @author Vincent Migot
 */
public final class SwaggerAPIGenerator {

    /**
     * Private constructor to avoid missuse of SwaggerAPIGenerator.
     */
    private SwaggerAPIGenerator() {

    }

    /**
     * Return full Swagger API for annotated classes found by Reflections.
     *
     * @param reflection Reflections instances for classes
     * @return Swagger API
     */
    public static synchronized Swagger getFullApi(Reflections reflection) {
        Swagger swagger = null;

        SwaggerContextService ctx = new SwaggerContextService();

        swagger = ctx.getSwagger();

        Map<String, Class<?>> availableAPI = OpenSilex.getAnnotatedClassesMap(Api.class, reflection);

        Set<Class<?>> classes = new HashSet<>(availableAPI.values());
        if (classes.size() > 0) {

            Reader reader = new Reader(swagger);
            swagger = reader.read(classes);

            return swagger;
        }

        return null;
    }

    /**
     * Return Swagger API for annotated classes found by Reflections in a specifi module.
     *
     * @param moduleClass Module class to limit API scope
     * @param reflection Reflections instances for classes
     * @return Swagger API
     */
    public static synchronized Swagger getModuleApi(Class<? extends OpenSilexModule> moduleClass, Reflections reflection) {
        Swagger swagger = null;

        SwaggerContextService ctx = new SwaggerContextService();

        swagger = ctx.getSwagger();

        Map<String, Class<?>> availableAPI = OpenSilex.getAnnotatedClassesMap(Api.class, reflection);

        String moduleID = ClassUtils.getProjectIdFromClass(moduleClass);

        Set<Class<?>> classes = new HashSet<>(availableAPI.values());

        Set<Class<?>> moduleClassesAPI = classes.stream().filter((Class<?> c) -> {
            String classModuleID = ClassUtils.getProjectIdFromClass(c);
            return moduleID.equals(classModuleID);
        }).collect(Collectors.toSet());

        if (moduleClassesAPI.size() > 0) {

            Reader reader = new Reader(swagger);
            swagger = reader.read(moduleClassesAPI);

            return swagger;
        }

        return null;
    }

    /**
     * Generate Swagger API for all java files in a specific folder.
     *
     * This API is filtered from global API using Java classes found in source or ti's sub-folder.
     *
     * @param source Base directory to look in
     * @param reflection Reflections instances for classes
     * @return Swagger API
     * @throws Exception
     */
    private static synchronized Swagger generate(String source, Reflections reflection) throws Exception {
        Swagger swagger = null;

        SwaggerContextService ctx = new SwaggerContextService();
        swagger = ctx.getSwagger();
        swagger.setHost("${host}");

        Set<Class<?>> classes = new HashSet<>();

        if (source != null) {
            Path sourcePath = Paths.get(source);
            if (sourcePath.toFile().exists()) {
                Map<String, Class<?>> availableAPI = OpenSilex.getAnnotatedClassesMap(Api.class, reflection);

                try (Stream<Path> walk = Files.walk(sourcePath)) {

                    walk.filter(Files::isRegularFile)
                            .forEach((Path p) -> {
                                String filename = p.getFileName().toString();

                                File filePath = p.toFile();
                                if (filePath.exists()) {
                                    String absoluteDirectory = filePath.getParent();
                                    String packageId = absoluteDirectory.substring(source.length()).replaceAll("\\\\|\\/", ".");

                                    if (filename.endsWith(".java")) {
                                        String className = packageId + "." + filename.substring(0, filename.length() - ".java".length());
                                        if (availableAPI.containsKey(className)) {
                                            classes.add(availableAPI.get(className));
                                        }
                                    }
                                }
                            });

                }
            }
        }

        if (classes.size() > 0) {

            Reader reader = new Reader(swagger);
            swagger = reader.read(classes);

            return swagger;
        }

        return null;
    }

    /**
     * Main entry point for swagger API generation.
     *
     * <pre>
     * Used by maven build to generate specific Swagger.json file for each OpenSilex module.
     * - First argument is the source folder
     * - Second argument is the destination swagger.json file produced
     * </pre>
     *
     * @param args command line arguments.
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        String source = args[0];
        String destination = args[1];

        OpenSilex instance = getOpenSilex(OpenSilex.getDefaultBaseDirectory());

        Reflections localRef = new Reflections(ConfigurationBuilder.build("")
                .setScanners(new TypeAnnotationsScanner(), new SubTypesScanner(), new MethodAnnotationsScanner())
                .setExpandSuperTypes(false)).merge(instance.getReflections());

        Swagger swagger = generate(source, localRef);

        if (swagger != null) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(Include.NON_NULL);
            File swaggerFile = new File(destination);
            swaggerFile.getParentFile().mkdirs();
            swaggerFile.createNewFile();
            mapper.writeValue(swaggerFile, swagger);
        }

        instance.shutdown();
    }

    /**
     * Return opensilex instance.
     *
     * @param baseDirectory
     * @return opensilex instance
     * @throws Exception
     */
    public static OpenSilex getOpenSilex(Path baseDirectory) throws Exception {
        Map<String, String> args = new HashMap<String, String>() {
            {
                put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.DEV_PROFILE_ID);

                // NOTE: uncomment this line to enable full debug during swagger API generation process
                // put(OpenSilex.DEBUG_ARG_KEY, "true");
            }
        };

        if (baseDirectory == null) {
            baseDirectory = OpenSilex.getDefaultBaseDirectory();
        }

        args.put(OpenSilex.BASE_DIR_ARG_KEY, baseDirectory.toFile().getCanonicalPath());

        return OpenSilex.createInstance(args);
    }
}
