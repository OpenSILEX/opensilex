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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opensilex.OpenSilexModule;

public class SwaggerAPIGenerator {

    public static synchronized Swagger getFullApi() {
        Swagger swagger = null;

        SwaggerContextService ctx = new SwaggerContextService();

        swagger = ctx.getSwagger();

        Map<String, Class<?>> availableAPI = ClassUtils.getAnnotatedClassesMap(Api.class);

        Set<Class<?>> classes = new HashSet<>(availableAPI.values());
        if (classes.size() > 0) {

            Reader reader = new Reader(swagger);
            swagger = reader.read(classes);

            return swagger;
        }

        return null;
    }
    
    public static synchronized Swagger getModuleApi(Class<? extends OpenSilexModule> moduleClass) {
        Swagger swagger = null;

        SwaggerContextService ctx = new SwaggerContextService();

        swagger = ctx.getSwagger();

        Map<String, Class<?>> availableAPI = ClassUtils.getAnnotatedClassesMap(Api.class);
        
        String moduleID = ClassUtils.getProjectIdFromClass(moduleClass);

        Set<Class<?>> classes = new HashSet<>(availableAPI.values());
        
        List<Class<?>> moduleClassesAPI = classes.stream().filter((Class<?> c) -> {
            String classModuleID = ClassUtils.getProjectIdFromClass(c);
            return moduleID.equals(classModuleID);
        }).collect(Collectors.toList());
        
        if (moduleClassesAPI.size() > 0) {

            Reader reader = new Reader(swagger);
            swagger = reader.read(classes);

            return swagger;
        }

        return null;
    }

    private static synchronized Swagger generate(String source) throws Exception {
        Swagger swagger = null;

        SwaggerContextService ctx = new SwaggerContextService();
        swagger = ctx.getSwagger();
        swagger.setHost("${host}");

        Set<Class<?>> classes = new HashSet<>();

        if (source != null) {
            Path sourcePath = Paths.get(source);
            if (sourcePath.toFile().exists()) {
                Map<String, Class<?>> availableAPI = ClassUtils.getAnnotatedClassesMap(Api.class);

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

        Swagger swagger = generate(source);

        if (swagger != null) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(Include.NON_NULL);
            File swaggerFile = new File(destination);
            swaggerFile.createNewFile();
            mapper.writeValue(swaggerFile, swagger);
        }
    }
}
