///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package org.opensilex.utils;
//
//import com.fasterxml.jackson.annotation.JsonAutoDetect;
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.net.URL;
//import java.nio.file.Path;
//import java.util.Collection;
//import java.util.HashSet;
//import java.util.Map;
//import java.util.Set;
//import org.opensilex.OpenSilex;
//import org.reflections.Reflections;
//import org.reflections.scanners.MethodAnnotationsScanner;
//import org.reflections.scanners.SubTypesScanner;
//import org.reflections.scanners.TypeAnnotationsScanner;
//import org.reflections.util.ConfigurationBuilder;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// *
// * @author vince
// */
//public class ReflectionsUtils {
//
//    private final static Logger LOGGER = LoggerFactory.getLogger(ReflectionsUtils.class);
//
//    private static ReflectionsUtils instance;
//
//    private Map<String, Map<String, Collection<String>>> storeMap;
//
//    private Set<URL> scannedURLs = new HashSet<>();
//
//    @JsonIgnoreProperties(value = {"configuration"})
//    private Reflections reflections;
//
//    private ReflectionsUtils() {
//
//    }
//
//    private ReflectionsUtils(Reflections reflections, Set<URL> scannedURLs) {
//        this.reflections = reflections;
//        this.scannedURLs = scannedURLs;
//    }
//
//    public static Reflections initReflections(Path baseDirectory, Set<URL> urlsToScan, boolean withCache) throws IOException {
//
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.setVisibility(
//                mapper.getSerializationConfig().
//                        getDefaultVisibilityChecker().
//                        withFieldVisibility(JsonAutoDetect.Visibility.ANY).
//                        withGetterVisibility(JsonAutoDetect.Visibility.NONE).
//                        withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
//        );
//
//        File reflectionFile = baseDirectory.resolve(".opensilex.reflections.cache").toFile();
//        ReflectionsUtils existingReflection = null;
//        if (withCache && reflectionFile.exists()) {
//            LOGGER.debug("Read reflection informations from cache file: " + reflectionFile.getCanonicalPath());
//            try {
//                existingReflection = mapper.readValue(new FileInputStream(reflectionFile), ReflectionsUtils.class);
//                if (!existingReflection.scannedURLs.containsAll(urlsToScan)
//                        || !urlsToScan.containsAll(existingReflection.scannedURLs)) {
//                    existingReflection = null;
//                }
//            } catch (IOException ex) {
//                LOGGER.error("Failure while reading reflection: " + reflectionFile.getCanonicalPath(), ex);
//            }
//        }
//
//        if (existingReflection == null) {
//            existingReflection = new ReflectionsUtils(loadReflection(urlsToScan), urlsToScan);
//        }
//
//        if (withCache) {
//            mapper.writerWithDefaultPrettyPrinter().writeValue(reflectionFile, existingReflection);
//        }
//
//        instance = existingReflection;
//
//        return instance.reflections;
//    }
//
//    private static Reflections loadReflection(Set<URL> urlsToScan) {
//        ConfigurationBuilder builder;
//        if (!urlsToScan.isEmpty()) {
//            builder = ConfigurationBuilder.build("", OpenSilex.getClassLoader())
//                    .setUrls(urlsToScan)
//                    .setScanners(new TypeAnnotationsScanner(), new SubTypesScanner(), new MethodAnnotationsScanner())
//                    .setExpandSuperTypes(false);
//        } else {
//            builder = ConfigurationBuilder.build("", OpenSilex.getClassLoader())
//                    .setScanners(new TypeAnnotationsScanner(), new SubTypesScanner(), new MethodAnnotationsScanner())
//                    .setExpandSuperTypes(false);
//        }
//
//        Reflections reflection = new Reflections(builder);
//
//        return reflection;
//    }
//
////    public static Reflections getReflectionInstance() {
////        if (instance == null) {
////            LOGGER.warn("Reflections utils should not be called before 'initReflections' method is called !");
////            return loadReflection(new HashSet());
////        }
////
////        return instance.reflections;
////    }
////    private static class ReflectionsSerializer extends JsonSerializer<Reflections> {
////
////        ObjectMapper mapper = new ObjectMapper();
////
////        @Override
////        public void serialize(Reflections t, JsonGenerator jg, SerializerProvider sp) throws IOException {
////        }
////
////    }
////
////    private static class ReflectionsDeserializer extends JsonDeserializer<Reflections> {
////
////        @Override
////        public Reflections deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
////
////        }
////
////    }
//}
