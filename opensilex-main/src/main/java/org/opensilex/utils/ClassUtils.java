//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.utils;

import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.UnescapedQuoteHandling;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilisy methods to work with classes, types and JAR files.
 *
 * @author Vincent Migot
 */
public final class ClassUtils {

    /**
     * Private constructor to avoid ClassUtils missuse.
     */
    private ClassUtils() {

    }

    /**
     * Class Logger.
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(ClassUtils.class);

    /**
     * Return true if given class is a primitive type (including String).
     *
     * @param type class to check
     * @return true if class is a primitive.
     */
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

    /**
     * Return true if given type is a primitive type (including String).
     *
     * @param type type to check
     * @return true if type is a primitive.
     */
    public static boolean isPrimitive(Type type) {
        return isPrimitive((Class<?>) type);
    }

    /**
     * Return true if given type is a generic type.
     *
     * @param type type to check
     * @return true if given type is generic
     */
    public static boolean isGenericType(Type type) {
        return (type instanceof ParameterizedType);
    }

    /**
     * Return first generic type class.
     *
     * @param type generic type to analyze
     * @return first generic type.
     */
    public static Type getGenericTypeParameter(ParameterizedType type) {
        return type.getActualTypeArguments()[0];
    }

    /**
     * Check if given type is a generic list.
     *
     * @param type type to check.
     * @return true if type is a list
     */
    public static boolean isGenericList(ParameterizedType type) {
        Class<?> typeClass = (Class<?>) type.getRawType();
        return isList(typeClass);
    }

    /**
     * Check if given class is a list.
     *
     * @param clazz class to check
     * @return true if class is a list
     */
    public static boolean isList(Class<?> clazz) {
        return List.class.isAssignableFrom(clazz);
    }

    /**
     * Check if given class is a map.
     *
     * @param clazz class to check
     * @return true if class is a map
     */
    public static boolean isMap(Class<?> clazz) {
        return Map.class.isAssignableFrom(clazz);
    }

    /**
     * Check if given class is equals to Class.class.
     *
     * @param clazz class to check
     * @return true if class is Class.class
     */
    public static boolean isClass(Class<?> clazz) {
        return clazz.equals(Class.class);
    }

    /**
     * Check if given class is an interface.
     *
     * @param clazz class to check
     * @return true if class is an interface
     */
    public static boolean isInterface(Class<?> clazz) {
        return clazz.isInterface();
    }

    /**
     * Return generic type from a field.
     *
     * @param field field to analyze
     * @return Generic field type or null
     */
    public static Class<?> getGenericTypeFromField(Field field) {
        if (isGenericType(field.getGenericType())) {
            ParameterizedType parameterized = (ParameterizedType) field.getGenericType();
            return (Class<?>) ClassUtils.getGenericTypeParameter(parameterized);
        }

        return null;
    }

    /**
     * Return generic type from a class.
     *
     * @param clazz class to analyze
     * @return Generic class type or null
     */
    public static Class<?> getGenericTypeFromClass(Class<?> clazz) {
        if (isGenericType(clazz)) {
            Type type = (Type) clazz;
            ParameterizedType parameterized = (ParameterizedType) type;
            return (Class<?>) ClassUtils.getGenericTypeParameter(parameterized);
        }

        return null;
    }

    /**
     * Return jar file where the given class belong to.
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
     * Return the root directory of the JAR file (or the classpath folder) corresponding to the given Class.
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

    /**
     * Return Maven pom file for the artifact containing given class.
     *
     * @param clazz class to look at
     * @return pom file
     * @throws IOException
     */
    public static File getPomFile(Class<?> clazz) throws IOException {
        return getPomFile(getJarFile(clazz), null, null);
    }

    /**
     * Return Maven pom file for the given JAR artifact file.
     *
     * @param jarFile jar to look in
     * @return pom file
     * @throws IOException
     */
    public static File getPomFile(File jarFile) throws IOException {
        return getPomFile(jarFile, null, null);
    }

    /**
     * Return Maven pom file for the given class, groupId and artifactId.
     *
     * @param clazz clazz to look for JAR
     * @param groupId artifact group identifier
     * @param artifactId artifact identifier
     * @return pom file
     * @throws IOException
     */
    public static File getPomFile(Class<?> clazz, String groupId, String artifactId) throws IOException {
        return getPomFile(getJarFile(clazz), groupId, artifactId);
    }

    /**
     * Return a file from within a JAR artifact corresponding to the given class.
     *
     * @param clazz clazz to look for JAR
     * @param filePath file path into JAR
     * @return pom file
     * @throws IOException
     */
    public static File getFileFromClassArtifact(Class<?> clazz, String filePath) throws IOException {
        return getFileFromJar(getJarFile(clazz), filePath);
    }

    /**
     * Return a file from within a JAR artifact.
     *
     * @param jarFile JAr to look into
     * @param filePath file path into JAR
     * @return file or null
     * @throws IOException
     */
    public static File getFileFromJar(File jarFile, String filePath) throws IOException {
        if (jarFile.isFile()) {
            ZipFile zipFile = new ZipFile(jarFile);
            ZipEntry entry = zipFile.getEntry(filePath);

            InputStream pomStream = zipFile.getInputStream(entry);
            File temp = File.createTempFile("opensilex-temp.", ".temp");
            temp.deleteOnExit();
            FileOutputStream out = new FileOutputStream(temp);
            byte[] buffer = new byte[BYTE_BUFFER];
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

    /**
     * Byte buffer constant.
     */
    private final static int BYTE_BUFFER = 1024;

    /**
     * Return pom file from a JAR.
     *
     * @param jarFile jar to look into
     * @param groupId
     * @param artifactId
     * @return pom file
     * @throws IOException
     */
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
            byte[] buffer = new byte[BYTE_BUFFER];
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

    /**
     * Find annotations on a class looking for super-classes until annotation is found.
     *
     * @param <T> annotation classto look for
     * @param objectClass Initial class to analyze.
     * @param annotationClass annotation class to look for
     * @return found annotation or null
     */
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

    /**
     * Get all fields for a class including from super classes.
     *
     * @param type base class to analyze.
     * @return List of fields
     */
    public static List<Field> getClassFieldsRecursivly(Class<?> type) {
        List<Field> fieldList = new ArrayList<>();

        getAllFields(fieldList, type);

        return fieldList;

    }

    /**
     * Help to execute method on each class fields (including super classes).
     *
     * @param type class to analyze
     * @param handler action to operate
     * @param rootClass stopping recursivity super-class
     */
    public static void executeOnClassFieldsRecursivly(Class<?> type, BiConsumer<Class<?>, Field> handler, Class<?> rootClass) {
        if (type.getSuperclass() != null && !type.equals(rootClass) && !type.getSuperclass().equals(Object.class)) {
            executeOnClassFieldsRecursivly(type.getSuperclass(), handler, rootClass);
        }

        for (Field f : type.getDeclaredFields()) {
            handler.accept(type, f);
        }
    }

    /**
     * Aggregate all fields for a class including from super classes.
     *
     * @param fields list of aggregated fields
     * @param type base class to analyze.
     */
    private static void getAllFields(List<Field> fields, Class<?> type) {
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }
    }

    /**
     * Convert a JAR url to a file.
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

    /**
     * Return project (artifact) identifier from a class.
     *
     * @param classFromProject class to use to find JAR/project/artifact
     * @return project identifier
     */
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

    /**
     * Return class constructor with parameter implementing a defined class.
     *
     * @param <T> class to inspect
     * @param classToInspect class to inspect
     * @param constructorParameterSuperClass Constructor parameter class to find.
     * @return Constructor for given class parameter or null
     */
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

    /**
     * List all files with a specific extension in a directory recursivly and execute action on them.
     *
     * @param directory base directory
     * @param extensionFilter file extension
     * @param action action to execute on found files
     * @throws IOException
     */
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

    /**
     * Determine if JAR file for a given class is a file or a directory (when running in a IDE).
     *
     * @param aClass class to retrieve corresponding JAR file.
     * @return true is corresponding JAR is a directory.
     */
    public static boolean isJarClassDirectory(Class<?> aClass) {
        return getJarFile(aClass).isDirectory();
    }
    
    /**
     * Get default csv parser settings (delimiter auto detection, ignore leading whitespaces, ignore leading trailing)
     * @return CsvParserSettings 
     */
    public static CsvParserSettings getCSVParserDefaultSettings() {
        CsvParserSettings csvParserSettings = new CsvParserSettings();
         // Configures the parser to analyze the input before parsing to discover the column delimiter character.
        csvParserSettings.setQuoteDetectionEnabled(true);
        csvParserSettings.setDelimiterDetectionEnabled(true, ',', ';', '\t', '|');
        csvParserSettings.setLineSeparatorDetectionEnabled(true);
        csvParserSettings.setUnescapedQuoteHandling(UnescapedQuoteHandling.STOP_AT_CLOSING_QUOTE);
        // detect separator on the first line
        csvParserSettings.setFormatDetectorRowSampleCount(1);
        // keep quote
//        csvParserSettings.setKeepQuotes(true);
        // trim
        csvParserSettings.trimValues(true); 
        // does not skip leading whitespaces 
        // does not skip trailing whitespaces 
        csvParserSettings.trimQuotedValues(true); 
        
        return  csvParserSettings;
    }
    
}
