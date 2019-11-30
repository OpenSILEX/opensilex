//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.utils;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.*;
import org.opensilex.server.rest.*;
import org.opensilex.sparql.model.*;
import org.reflections.*;
import org.reflections.util.*;
import org.slf4j.*;

/**
 *
 * @author vincent
 */
public class ClassInfo {

    private final static Logger LOGGER = LoggerFactory.getLogger(ClassInfo.class);

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
            return (Class<?>) ClassInfo.getGenericTypeParameter(parameterized);
        }

        return null;
    }

    public static Class<?> getGenericTypeFromClass(Class<?> clazz) {
        if (isGenericType(clazz)) {
            Type type = (Type) clazz;
            ParameterizedType parameterized = (ParameterizedType) type;
            return (Class<?>) ClassInfo.getGenericTypeParameter(parameterized);
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
                File pom = ClassInfo.getPomFile(classFromProject);
                MavenXpp3Reader reader = new MavenXpp3Reader();
                Model model = reader.read(new FileReader(pom));
                projectId = model.getArtifactId();
            } catch (Exception ex) {
                return "";
            }
        }

        return projectId;
    }

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

    public static Reflections getReflectionInstance() {
        if (reflections == null) {
            reflections = new Reflections(ConfigurationBuilder.build("", Thread.currentThread().getContextClassLoader()).setExpandSuperTypes(false));
        }

        return reflections;
    }

    public static Set<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotation) {
        return getReflectionInstance().getTypesAnnotatedWith(annotation);
    }

    public static Map<String, Class<?>> getAnnotatedClassesMap(Class<? extends Annotation> annotation) {
        Map<String, Class<?>> classMap = new HashMap<>();

        getReflectionInstance().getTypesAnnotatedWith(annotation).forEach((Class<?> c) -> {
            LOGGER.debug("Annoted class found: " + annotation.getCanonicalName());
            classMap.put(c.getCanonicalName(), c);
        });
        
        return classMap;
    }
}
