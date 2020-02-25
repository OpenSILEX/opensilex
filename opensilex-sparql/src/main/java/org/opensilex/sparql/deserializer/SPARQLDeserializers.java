//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.deserializer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import org.apache.jena.graph.Node;
import org.opensilex.OpenSilex;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vincent
 */
public class SPARQLDeserializers {

    private final static Logger LOGGER = LoggerFactory.getLogger(SPARQLDeserializers.class);

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
        ServiceLoader.load(SPARQLDeserializer.class, OpenSilex.getClassLoader())
                .forEach(deserializers::add);

        for (SPARQLDeserializer<?> deserializer : deserializers) {
            try {
                Class<?> key = parameterizedClass(deserializer, SPARQLDeserializer.class, 0);
                deserializersMap.put(key, deserializer);
            } catch (ClassNotFoundException ex) {
                LOGGER.error("SPARQL deserializer not found (should never happend)", ex);
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

    public static Node nodeURI(URI uri) throws SPARQLInvalidURIException {
        try {
            return SPARQLDeserializers.getForClass(URI.class).getNodeFromString(uri.toString());
        } catch (Exception ex) {
            throw new SPARQLInvalidURIException(uri);
        }
    }

    public static String getExpandedURI(String value) {
        return URIDeserializer.getExpandedURI(value);
    }

    public static List<Node> nodeListURI(List<URI> uris) throws Exception {
        SPARQLDeserializer<URI> uriParser = SPARQLDeserializers.getForClass(URI.class);
        List<Node> uriNodes = new ArrayList<>();
        for (URI uri : uris) {
            uriNodes.add(uriParser.getNodeFromString(uri.toString()));
        }

        return uriNodes;
    }

}
