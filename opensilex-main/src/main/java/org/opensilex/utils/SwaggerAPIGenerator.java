package org.opensilex.utils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
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

public class SwaggerAPIGenerator {

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
                                        String className = packageId + "." + filename.substring(0, filename.length() - 5);
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
            swaggerFile.createNewFile();
            mapper.writeValue(swaggerFile, swagger);
        }
    }

    public static OpenSilex getOpenSilex(Path baseDirectory) throws Exception {
        Map<String, String> args = new HashMap<String, String>() {
            {
                put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.DEV_PROFILE_ID);
                put(OpenSilex.DEBUG_ARG_KEY, "true");
            }
        };

        if (baseDirectory == null) {
            baseDirectory = OpenSilex.getDefaultBaseDirectory();
        }

        args.put(OpenSilex.BASE_DIR_ARG_KEY, baseDirectory.toFile().getCanonicalPath());

        return OpenSilex.createInstance(args, false, true);
    }
}
