/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.utils.deserializer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 *
 * @author vincent
 */
public class Deserializers {

    private static Map<Class<?>, Deserializer<?>> deserialisersMap;
    
    private static Map<Class<?>, Deserializer<?>> getDeserializerMap() {
        if (deserialisersMap == null) {
            deserialisersMap = buildDeserializersMap();
        }

        return deserialisersMap;
    }

    public static boolean existsForClass(Class<?> clazz) {
        Map<Class<?>, Deserializer<?>> map = getDeserializerMap();
        return map.containsKey(clazz);
    }
        
    @SuppressWarnings("unchecked")
    public static <T> Deserializer<T> getForClass(Class<T> clazz) throws DeserializerNotFoundException {
        Map<Class<?>, Deserializer<?>> map = getDeserializerMap();
        if (existsForClass(clazz)) {
            return (Deserializer<T>) map.get(clazz);
        } else {
            throw new DeserializerNotFoundException(clazz);
        }

    }

    private static Map<Class<?>, Deserializer<?>> buildDeserializersMap() {
        HashMap<Class<?>, Deserializer<?>> deserializersMap = new HashMap<>();

        List<Deserializer<?>> deserializers = new ArrayList<>();
        ServiceLoader.load(Deserializer.class, Thread.currentThread().getContextClassLoader())
                .forEach(deserializers::add);
        
        for (Deserializer<?> deserializer : deserializers) {
            try {
                Class<?> key = parameterizedClass(deserializer, Deserializer.class, 0);
                deserializersMap.put(key, deserializer);
            } catch (ClassNotFoundException ex) {
                // TODO
            }
        }

        return deserializersMap;
    }

    private static Class<?> parameterizedClass(final Class<?> root, final Class<?> target, final int paramIndex)
            throws ClassNotFoundException {

        final Type[] sooper = root.getGenericInterfaces();
        for (final Type t : sooper) {
            if (!(t instanceof ParameterizedType)) {
                continue;
            }
            final ParameterizedType type = ((ParameterizedType) t);
            if (type.getRawType().getTypeName().equals(target.getTypeName())) {
                return Class.forName(type.getActualTypeArguments()[paramIndex].getTypeName());
            }
        }
        for (final Class<?> parent : root.getInterfaces()) {
            final Class<?> result = parameterizedClass(parent, target, paramIndex);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private static Class<?> parameterizedClass(final Object object, final Class<?> target, final int paramIndex)
            throws ClassNotFoundException {
        return parameterizedClass(object.getClass(), target, paramIndex);
    }

}
