package org.opensilex.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.opensilex.OpenApiExtension;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModule;
import org.opensilex.server.rest.serialization.GeoJsonConverter;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class to generate OpenAPI 3.1.1 specification file.
 *
 * @author Vincent Migot
 */
public final class SwaggerAPIGenerator {

    public static class JenaAnnotationIntrospector extends JacksonAnnotationIntrospector {
        @Override
        public boolean hasIgnoreMarker(AnnotatedMember m) {
            if (m != null && "setNsPrefixes".equals(m.getName())) {
                return true;
            }
            return super.hasIgnoreMarker(m);
        }
    }

    @JsonIgnoreType
    public abstract static class JenaIgnoreTypeMixin {
    }

    private static boolean configured = false;

    public static synchronized void configureModelConverters() {
        if (configured) {
            return;
        }
        configured = true;

        ObjectMapper jsonMapper = Json.mapper();
        ObjectMapper yamlMapper = Yaml.mapper();

        AnnotationIntrospector intro = new JenaAnnotationIntrospector();
        jsonMapper.setAnnotationIntrospector(AnnotationIntrospector.pair(intro, jsonMapper.getSerializationConfig().getAnnotationIntrospector()));
        yamlMapper.setAnnotationIntrospector(AnnotationIntrospector.pair(intro, yamlMapper.getSerializationConfig().getAnnotationIntrospector()));

        Class<?>[] jenaTypes = new Class<?>[] {
            org.apache.jena.rdf.model.Model.class,
            org.apache.jena.shared.PrefixMapping.class,
            org.apache.jena.rdf.model.RDFNode.class,
            org.apache.jena.rdf.model.Resource.class,
            org.apache.jena.rdf.model.Property.class,
            org.apache.jena.rdf.model.Statement.class,
            org.apache.jena.rdf.model.RDFList.class
        };

        for (Class<?> jenaType : jenaTypes) {
            jsonMapper.addMixIn(jenaType, JenaIgnoreTypeMixin.class);
            yamlMapper.addMixIn(jenaType, JenaIgnoreTypeMixin.class);
        }

        List<ModelConverter> existing = new ArrayList<>(ModelConverters.getInstance().getConverters());
        for (ModelConverter mc : existing) {
            ModelConverters.getInstance().removeConverter(mc);
        }
        ModelConverters.getInstance().addConverter(new JenaModelConverter());
        ModelConverters.getInstance().addConverter(new ModelResolver(jsonMapper));
        for (ModelConverter mc : existing) {
            if (!(mc instanceof ModelResolver)) {
                ModelConverters.getInstance().addConverter(mc);
            }
        }
    }

    static {
        configureModelConverters();
    }

    private SwaggerAPIGenerator() {
    }

    public static synchronized OpenAPI getFullApi(Reflections reflection) {
        configureModelConverters();
        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new Info().title("OpenSilex API").version("1.0.0"));

        Map<String, Class<?>> availableAPI = OpenSilex.getAnnotatedClassesMap(Tag.class, reflection);
        Set<Class<?>> classes = new HashSet<>(availableAPI.values());

        Reader reader = new Reader(openAPI);
        if (!classes.isEmpty()) {
            openAPI = reader.read(classes);
        }
        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new Components());
        }
        if (openAPI.getPaths() == null) {
            openAPI.setPaths(new io.swagger.v3.oas.models.Paths());
        }
        GeoJsonConverter.injectGeoJsonSchema(openAPI);
        return openAPI;
    }

    public static synchronized OpenAPI getModuleApi(Class<? extends OpenSilexModule> moduleClass, Reflections reflection) {
        return getModuleApi(ClassUtils.getProjectIdFromClass(moduleClass), reflection);
    }

    public static synchronized OpenAPI getModuleApi(String moduleID, Reflections reflection) {
        configureModelConverters();
        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new Info().title("OpenSilex API - " + moduleID).version("1.0.0"));

        Map<String, Class<?>> availableAPI = OpenSilex.getAnnotatedClassesMap(Tag.class, reflection);
        Set<Class<?>> classes = new HashSet<>(availableAPI.values());

        Set<Class<?>> moduleClassesAPI = classes.stream().filter((Class<?> c) -> {
            String classModuleID = ClassUtils.getProjectIdFromClass(c);
            return moduleID.equals(classModuleID);
        }).collect(Collectors.toSet());

        if (!moduleClassesAPI.isEmpty()) {
            Reader reader = new Reader(openAPI);
            openAPI = reader.read(moduleClassesAPI);
            GeoJsonConverter.injectGeoJsonSchema(openAPI);
            return openAPI;
        }

        return null;
    }

    private static synchronized OpenAPI generate(String source, Reflections reflection, List<Class<?>> additionalDefinitions) throws Exception {
        configureModelConverters();
        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new Info().title("OpenSilex API").version("1.0.0"));

        Set<Class<?>> classes = new HashSet<>();

        if (source != null) {
            Path sourcePath = Paths.get(source);
            if (sourcePath.toFile().exists()) {
                Map<String, Class<?>> availableAPI = OpenSilex.getAnnotatedClassesMap(Tag.class, reflection);

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

        Reader reader = new Reader(openAPI);
        if (!classes.isEmpty()) {
            openAPI = reader.read(classes);
        }

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new Components());
        }

        if (openAPI.getPaths() == null) {
            openAPI.setPaths(new io.swagger.v3.oas.models.Paths());
        }

        if (additionalDefinitions != null) {
            for (Class<?> model : additionalDefinitions) {
                Map<String, Schema> map = ModelConverters.getInstance().read(model);
                for (Map.Entry<String, Schema> entry : map.entrySet()) {
                    openAPI.getComponents().addSchemas(entry.getKey(), entry.getValue());
                }
            }
        }

        GeoJsonConverter.injectGeoJsonSchema(openAPI);
        return openAPI;
    }

    public static void main(String[] args) throws Exception {
        String source = args[0];
        String destination = args[1];

        OpenSilex instance = getOpenSilex(OpenSilex.getDefaultBaseDirectory());
        var modelList = new ArrayList<Class<?>>();
        instance.getModulesImplementingInterface(OpenApiExtension.class).forEach(module -> {
            List<Class<?>> additional = module.getAdditionalOpenApiDefinitions();
            if (additional != null) {
                modelList.addAll(additional);
            }
        });

        Reflections instanceRef = instance.getReflections();
        Reflections fallbackRef = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forClassLoader(OpenSilex.getClassLoader()))
                .setScanners(new TypeAnnotationsScanner(), new SubTypesScanner(), new MethodAnnotationsScanner()));

        Reflections localRef;
        if (instanceRef != null) {
            localRef = instanceRef.merge(fallbackRef);
        } else {
            localRef = fallbackRef;
        }

        OpenAPI openAPI = generate(source, localRef, modelList);

        if (openAPI != null) {
            File openApiFile = new File(destination);
            if (openApiFile.getParentFile() != null) {
                openApiFile.getParentFile().mkdirs();
            }
            openApiFile.createNewFile();
            Json.pretty().writeValue(openApiFile, openAPI);
        }

        instance.shutdown();
    }

    public static OpenSilex getOpenSilex(Path baseDirectory) throws Exception {
        Map<String, String> args = new HashMap<>() {
            {
                put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.INTERNAL_OPERATIONS_PROFILE_ID);
            }
        };

        if (baseDirectory == null) {
            baseDirectory = OpenSilex.getDefaultBaseDirectory();
        }

        args.put(OpenSilex.BASE_DIR_ARG_KEY, baseDirectory.toFile().getCanonicalPath());

        return OpenSilex.createInstance(args);
    }

    /**
     * Custom ModelConverter to prevent Jackson introspection errors on Apache Jena & RDF4J classes.
     */
    public static class JenaModelConverter implements ModelConverter {

        @Override
        public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
            if (type != null && type.getType() != null) {
                Class<?> rawClass = null;
                if (type.getType() instanceof Class<?>) {
                    rawClass = (Class<?>) type.getType();
                } else if (type.getType() instanceof java.lang.reflect.ParameterizedType) {
                    java.lang.reflect.Type raw = ((java.lang.reflect.ParameterizedType) type.getType()).getRawType();
                    if (raw instanceof Class<?>) {
                        rawClass = (Class<?>) raw;
                    }
                }
                if (rawClass != null && (rawClass.getName().startsWith("org.apache.jena.") || rawClass.getName().startsWith("org.eclipse.rdf4j."))) {
                    ObjectSchema schema = new ObjectSchema();
                    schema.setName(rawClass.getSimpleName());
                    schema.setDescription("RDF model element (" + rawClass.getSimpleName() + ")");
                    return schema;
                }
            }
            if (chain != null && chain.hasNext()) {
                return chain.next().resolve(type, context, chain);
            }
            return null;
        }
    }
}
