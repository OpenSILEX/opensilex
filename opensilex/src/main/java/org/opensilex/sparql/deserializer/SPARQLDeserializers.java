//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.deserializer;

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
public class SPARQLDeserializers {

    private static Map<Class<?>, SPARQLDeserializer<?>> deserialisersMap;
    
    private static Map<Class<?>, SPARQLDeserializer<?>> getDeserializerMap() {
        if (deserialisersMap == null) {
            deserialisersMap = buildDeserializersMap();
        }

        return deserialisersMap;
    }

    public static boolean existsForClass(Class<?> clazz) {
        Map<Class<?>, SPARQLDeserializer<?>> map = getDeserializerMap();
        return map.containsKey(clazz);
    }
        
    @SuppressWarnings("unchecked")
    public static <T> SPARQLDeserializer<T> getForClass(Class<T> clazz) throws SPARQLDeserializerNotFoundException {
        Map<Class<?>, SPARQLDeserializer<?>> map = getDeserializerMap();
        if (existsForClass(clazz)) {
            return (SPARQLDeserializer<T>) map.get(clazz);
        } else {
            throw new SPARQLDeserializerNotFoundException(clazz);
        }

    }
    
    private static Map<Class<?>, SPARQLDeserializer<?>> buildDeserializersMap() {
        HashMap<Class<?>, SPARQLDeserializer<?>> deserializersMap = new HashMap<>();

        List<SPARQLDeserializer<?>> deserializers = new ArrayList<>();
        ServiceLoader.load(SPARQLDeserializer.class, Thread.currentThread().getContextClassLoader())
                .forEach(deserializers::add);
        
        for (SPARQLDeserializer<?> deserializer : deserializers) {
            try {
                Class<?> key = parameterizedClass(deserializer, SPARQLDeserializer.class, 0);
                deserializersMap.put(key, deserializer);
            } catch (ClassNotFoundException ex) {
                // TODO warn on missing deserializer. Error ?
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
