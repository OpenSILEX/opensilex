/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 *
 * @author vincent
 */
public class ClassInfo {

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
     * Return main jar file or a directory if application is run from sources
     *
     * @return Root directory
     */
    public static File getJarFile(Class<?> c) {
        return new File(
                c.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .getPath()
        );

    }

    /**
     * Return main jar root directory
     *
     * @return Root directory
     */
    public static Path getBaseDirectory(Class<?> c) {
        File rootFile = getJarFile(c);

        String rootDirectory;
        if (rootFile.isFile()) {
            rootDirectory = rootFile.getParent();
        } else {
            rootDirectory = rootFile.getPath();
        }

        return Paths.get(rootDirectory);
    }

    public static File getPomFile(Class<?> c) throws IOException {
        return getPomFile(getJarFile(c), null, null);
    }

    public static File getPomFile(File jarFile) throws IOException {
        return getPomFile(jarFile, null, null);
    }

    public static File getPomFile(Class<?> c, String groupId, String artifactId) throws IOException {
        return getPomFile(getJarFile(c), groupId, artifactId);
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

}
