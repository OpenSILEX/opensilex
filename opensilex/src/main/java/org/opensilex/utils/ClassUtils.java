//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.opensilex.OpenSilex;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vincent
 */
public class ClassUtils {

    private final static Logger LOGGER = LoggerFactory.getLogger(ClassUtils.class);

    public static boolean isPrimitive(Class<?> type) {
        return type.isPrimitive()
                || type.equals(Boolean.class)
                || type.equals(Byte.class)
                || type.equals(Character.class)
                || type.equals(Short.class)
                || type.equals(Integer.class)
                || type.equals(Long.class)
                || type.equals(Float.class)
                || type.equals(Double.class)
                || type.equals(String.class);
    }

    public static boolean isPrimitive(Type type) {
        return isPrimitive((Class<?>) type);
    }

    public static boolean isGenericType(Type type) {
        return (type instanceof ParameterizedType);
    }

    public static Type getGenericTypeParameter(ParameterizedType type) {
        return type.getActualTypeArguments()[0];
    }

    public static boolean isGenericList(ParameterizedType type) {
        Class<?> typeClass = (Class<?>) type.getRawType();
        return isList(typeClass);
    }

    public static boolean isList(Class<?> clazz) {
        return List.class.isAssignableFrom(clazz);
    }

    public static boolean isMap(Class<?> clazz) {
        return Map.class.isAssignableFrom(clazz);
    }

    public static boolean isClass(Class<?> clazz) {
        return clazz.equals(Class.class);
    }

    public static boolean isInterface(Class<?> clazz) {
        return clazz.isInterface();
    }

    public static Class<?> getGenericTypeFromField(Field field) {
        if (isGenericType(field.getGenericType())) {
            ParameterizedType parameterized = (ParameterizedType) field.getGenericType();
            return (Class<?>) ClassUtils.getGenericTypeParameter(parameterized);
        }

        return null;
    }

    public static Class<?> getGenericTypeFromClass(Class<?> clazz) {
        if (isGenericType(clazz)) {
            Type type = (Type) clazz;
            ParameterizedType parameterized = (ParameterizedType) type;
            return (Class<?>) ClassUtils.getGenericTypeParameter(parameterized);
        }

        return null;
    }

    /**
     * Return jar file where the given class belong to
     *
     * @param clazz Class from which this method find JAR file
     * @return The corresponding JAR file
     */
    public static File getJarFile(Class<?> clazz) {
        return new File(
                clazz.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .getPath()
        );

    }

    /**
     * Return the root directory of the JAR file (or the classpath folder)
     * corresponding to the given Class
     *
     * @param clazz The class to look at
     * @return Class base directory
     */
    public static Path getBaseDirectory(Class<?> clazz) {
        File rootFile = getJarFile(clazz);

        String rootDirectory;
        if (rootFile.isFile()) {
            rootDirectory = rootFile.getParent();
        } else {
            rootDirectory = rootFile.getPath();
        }

        return Paths.get(rootDirectory);
    }

    public static File getPomFile(Class<?> clazz) throws IOException {
        return getPomFile(getJarFile(clazz), null, null);
    }

    public static File getPomFile(File jarFile) throws IOException {
        return getPomFile(jarFile, null, null);
    }

    public static File getPomFile(Class<?> clazz, String groupId, String artifactId) throws IOException {
        return getPomFile(getJarFile(clazz), groupId, artifactId);
    }

    
    public static File getFileFromClassArtifact(Class<?> clazz, String filePath) throws IOException {
        return getFileFromJar(getJarFile(clazz), filePath);
    }
    
    public static File getFileFromJar(File jarFile, String filePath) throws IOException {
        if (jarFile.isFile()) {
            ZipFile zipFile = new ZipFile(jarFile);
            ZipEntry entry = zipFile.getEntry(filePath);

            InputStream pomStream = zipFile.getInputStream(entry);
            File temp = File.createTempFile("opensilex-temp.", ".temp");
            temp.deleteOnExit();
            FileOutputStream out = new FileOutputStream(temp);
            byte[] buffer = new byte[1024];
            int bytesRead;
            //read from is to buffer
            while ((bytesRead = pomStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            pomStream.close();
            out.flush();
            out.close();
            zipFile.close();
            return temp;
        } else {
            return Paths.get(jarFile.getAbsolutePath()).resolve(filePath).toFile();
        }
    }

    public static File getPomFile(File jarFile, String groupId, String artifactId) throws IOException {

        if (jarFile.isFile()) {
            ZipFile zipFile = new ZipFile(jarFile);
            ZipEntry entry;
            if (groupId != null && artifactId != null) {
                entry = zipFile.getEntry("META-INF/maven/" + groupId + "/" + artifactId + "/pom.xml");
            } else {
                entry = zipFile.getEntry("opensilex-pom.xml");
            }

            if (entry == null) {
                throw new IOException(groupId + ":" + artifactId + " - POM file not found in:" + jarFile.getAbsolutePath());
            }

            InputStream pomStream = zipFile.getInputStream(entry);
            File pom = File.createTempFile("opensilex-pom.", ".xml");
            pom.deleteOnExit();
            FileOutputStream out = new FileOutputStream(pom);
            byte[] buffer = new byte[1024];
            int bytesRead;
            //read from is to buffer
            while ((bytesRead = pomStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            pomStream.close();
            out.flush();
            out.close();
            zipFile.close();
            return pom;
        } else {
            return Paths.get(jarFile.getAbsolutePath()).resolve("opensilex-pom.xml").toFile();
        }

    }

    public static <T extends Annotation> T findClassAnnotationRecursivly(Class<?> objectClass, Class<T> annotationClass) {
        Class<?> type = objectClass;
        T annotation = null;
        while (type != null) {
            annotation = type.getAnnotation(annotationClass);
            if (annotation != null) {
                break;
            }
            type = type.getSuperclass();
        }

        return annotation;

    }

    public static List<Field> getClassFieldsRecursivly(Class<?> type) {
        List<Field> fieldList = new ArrayList<>();

        getAllFields(fieldList, type);

        return fieldList;

    }

    /**
     * Convert a JAR url to a file
     *
     * @param jarURL Jar URL
     * @return Jar file
     */
    public static File getJarFileFromURL(URL jarURL) {
        File jarFile;
        try {
            jarFile = new File(jarURL.toURI());
        } catch (URISyntaxException e) {
            jarFile = new File(jarURL.getPath());
        }

        return jarFile;
    }

    private static void getAllFields(List<Field> fields, Class<?> type) {
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }
    }

    public static String getProjectIdFromClass(Class<?> classFromProject) {
        String projectId = classFromProject.getPackage().getImplementationTitle();
        if (projectId == null) {
            try {
                File pom = ClassUtils.getPomFile(classFromProject);
                MavenXpp3Reader reader = new MavenXpp3Reader();
                Model model = reader.read(new FileReader(pom));
                projectId = model.getArtifactId();
            } catch (Exception ex) {
                return "";
            }
        }

        return projectId;
    }

    @SuppressWarnings("unchecked")
    public static <T> Constructor<T> getConstructorWithParameterImplementing(Class<T> classToInspect, Class<?> constructorParameterSuperClass) {
        for (Constructor<?> constructor : classToInspect.getConstructors()) {
            if (constructor.getParameterCount() == 1) {
                if (constructorParameterSuperClass.isAssignableFrom(constructor.getParameters()[0].getType())) {
                    return (Constructor<T>) constructor;
                }
            }
        }

        return null;
    }

    private static Reflections reflections;

    private static final Set<URL> REFLECTION_URLS = new HashSet<>();

    public static void addURLToScan(URL url) {
        reflections = null;
        REFLECTION_URLS.add(url);
    }

    public static Reflections getReflectionInstance() {
        if (reflections == null) {
            if (!REFLECTION_URLS.isEmpty()) {
                reflections = new Reflections(
                        ConfigurationBuilder.build("", OpenSilex.getClassLoader())
                                .setUrls(REFLECTION_URLS)
                                .setScanners(new TypeAnnotationsScanner(), new SubTypesScanner(), new MethodAnnotationsScanner())
                                .setExpandSuperTypes(false)
                );
            } else {
                reflections = new Reflections(
                        ConfigurationBuilder.build("", OpenSilex.getClassLoader())
                                .setScanners(new TypeAnnotationsScanner(), new SubTypesScanner(), new MethodAnnotationsScanner())
                                .setExpandSuperTypes(false)
                );
            }
        }

        return reflections;
    }

    public static Set<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotation) {
        return getReflectionInstance().getTypesAnnotatedWith(annotation);
    }

    public static Set<Method> getAnnotatedMethods(Class<? extends Annotation> annotation) {
        return getReflectionInstance().getMethodsAnnotatedWith(annotation);
    }

    public static Map<String, Class<?>> getAnnotatedClassesMap(Class<? extends Annotation> annotation) {
        Map<String, Class<?>> classMap = new HashMap<>();

        getReflectionInstance().getTypesAnnotatedWith(annotation).forEach((Class<?> c) -> {
            LOGGER.debug("Annoted class found: " + annotation.getCanonicalName() + " in " + c.getCanonicalName());
            classMap.put(c.getCanonicalName(), c);
        });

        return classMap;
    }

    public static void listFilesByExtension(String directory, String extensionFilter, Consumer<File> action) throws IOException {
        Path directoryPath = Paths.get(directory);

        LOGGER.debug("Load files by extension: " + directory + " " + extensionFilter);

        if (Files.exists(directoryPath) && Files.isDirectory(directoryPath)) {
            Files.walk(directoryPath)
                    .filter(Files::isRegularFile)
                    .map(p -> p.toFile())
                    .filter(f -> f.getAbsolutePath().endsWith("." + extensionFilter))
                    .forEach(action);
        }
    }
}
