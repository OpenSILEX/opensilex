/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opensilex.service.Service;
import org.opensilex.service.ServiceConnection;
import org.opensilex.utils.ClassInfo;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vincent
 */
public class ConfigProxyHandler implements InvocationHandler {

    private final static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);

    private final String baseDirectory;
    private final JsonNode rootNode;
    private final ObjectMapper yamlMapper;

    public ConfigProxyHandler(String key, JsonNode rootNode, ObjectMapper yamlMapper) {
        if (key.startsWith("/") || key.isEmpty()) {
            this.baseDirectory = key + "/";
        } else {
            this.baseDirectory = "/" + key + "/";
        }
        this.rootNode = rootNode;
        this.yamlMapper = yamlMapper;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws InvalidConfigException {
        String key = baseDirectory + method.getName();

        return nodeToObject(method.getGenericReturnType(), key, rootNode, method);
    }

    private Object nodeToObject(Type type, String key, JsonNode node, Method method) throws InvalidConfigException {
        Object result;

        if (ClassInfo.isGenericType(type)) {
            ParameterizedType genericReturnType = (ParameterizedType) type;
            Type genericParameter = genericReturnType.getActualTypeArguments()[0];

            try {
                Class<?> returnTypeClass = (Class<?>) genericReturnType.getRawType();
                if (ClassInfo.isList(returnTypeClass)) {
                    result = getList(genericParameter, node.at(key), method);
                } else if (ClassInfo.isMap(returnTypeClass)) {
                    Type genericValueParameter = genericReturnType.getActualTypeArguments()[1];
                    result = getMap(genericValueParameter, node.at(key), method);
                } else if (ClassInfo.isClass(returnTypeClass)) {
                    result = getClassDefinition(node.at(key), method);
                } else {
                    throw new InvalidConfigException(
                            "Can't get configuration property: " + key
                            + " with given generic type: " + returnTypeClass.getTypeName()
                            + " only List and Map can be used as parametrized configuration class"
                    );
                }
            } catch (ClassNotFoundException ex) {
                throw new InvalidConfigException(
                        "Can't get configuration property: " + key
                        + " class not found: " + node.at(key).asText()
                );
            } catch (IOException ex) {
                throw new InvalidConfigException(
                        "Can't get configuration property: " + key
                        + " with given generic type: " + genericReturnType.getTypeName()
                        + ex.getMessage()
                );
            }
        } else {
            Class<?> returnTypeClass = (Class<?>) type;
            if (ClassInfo.isPrimitive(returnTypeClass)) {
                result = getPrimitive(returnTypeClass.getCanonicalName(), node.at(key), method);
            } else if (Service.class.isAssignableFrom(returnTypeClass)) {
                result = getService(returnTypeClass, node.at(key), method);
            } else if (ClassInfo.isInterface(returnTypeClass)) {
                result = getInterface(returnTypeClass, key, node);
            } else {

                throw new InvalidConfigException(
                        "Can't get configuration property: " + key
                        + " with given type: " + returnTypeClass.getCanonicalName()
                        + "Configuration field must be a primitive type, a string, a list,"
                        + "a map or an interface composed of the same kind of fields recursively"
                );
            }
        }
        return result;
    }

    public static Object getPrimitive(String type, JsonNode node, Method method) {
        Object result = null;

        switch (type) {
            case "boolean":
            case "java.lang.Boolean":
                result = getBoolean(node, method);
                break;
            case "int":
            case "java.lang.Integer":
                result = getInt(node, method);
                break;
            case "long":
            case "java.lang.Long":
                result = getLong(node, method);
                break;
            case "float":
            case "java.lang.Float":
                result = getFloat(node, method);
                break;
            case "double":
            case "java.lang.Double":
                result = getDouble(node, method);
                break;
            case "char":
            case "java.lang.Character":
                result = getChar(node, method);
                break;
            case "short":
            case "java.lang.Short":
                result = getShort(node, method);
                break;
            case "byte":
            case "java.lang.Byte":
                result = getByte(node, method);
                break;
            case "java.lang.String":
                result = getString(node, method);
                break;
            default:
                break;
        }
        return result;
    }

    private static boolean getBoolean(JsonNode node, Method method) {
        boolean result = false;
        if (node.isMissingNode() || node.isNull()) {
            if (method != null) {
                ConfigDescription annotation = method.getAnnotation(ConfigDescription.class);
                if (annotation != null) {
                    result = annotation.defaultBoolean();
                }
            }
        } else {
            result = node.asBoolean();
        }

        return result;
    }

    private static int getInt(JsonNode node, Method method) {
        int result = 0;
        if (node.isMissingNode() || node.isNull()) {
            if (method != null) {
                ConfigDescription annotation = method.getAnnotation(ConfigDescription.class);
                if (annotation != null) {
                    result = annotation.defaultInt();
                }
            }
        } else {
            result = node.asInt();
        }

        return result;
    }

    private static long getLong(JsonNode node, Method method) {
        long result = 0L;
        if (node.isMissingNode() || node.isNull()) {
            if (method != null) {
                ConfigDescription annotation = method.getAnnotation(ConfigDescription.class);
                if (annotation != null) {
                    result = annotation.defaultLong();
                }
            }
        } else {
            result = node.asLong();
        }

        return result;
    }

    private static float getFloat(JsonNode node, Method method) {
        float result = 0;
        String nodeText = node.asText();
        if (node.isMissingNode() || node.isNull()) {
            if (method != null) {
                ConfigDescription annotation = method.getAnnotation(ConfigDescription.class);
                if (annotation != null) {
                    result = annotation.defaultFloat();
                }
            }
        } else {
            result = Float.valueOf(nodeText);
        }

        return result;
    }

    private static double getDouble(JsonNode node, Method method) {
        double result = 0;
        if (node.isMissingNode() || node.isNull()) {
            if (method != null) {
                ConfigDescription annotation = method.getAnnotation(ConfigDescription.class);
                if (annotation != null) {
                    result = annotation.defaultDouble();
                }
            }
        } else {
            result = node.asDouble();
        }

        return result;
    }

    private static char getChar(JsonNode node, Method method) {
        char result = Character.MIN_VALUE;
        String nodeText = node.asText();
        if (node.isMissingNode() || node.isNull()) {
            if (method != null) {
                ConfigDescription annotation = method.getAnnotation(ConfigDescription.class);
                if (annotation != null) {
                    result = annotation.defaultChar();
                }
            }
        } else {
            result = nodeText.charAt(0);
        }

        return result;
    }

    private static short getShort(JsonNode node, Method method) {
        short result = (short) 0;
        String nodeText = node.asText();
        if (node.isMissingNode() || node.isNull()) {
            if (method != null) {
                ConfigDescription annotation = method.getAnnotation(ConfigDescription.class);
                if (annotation != null) {
                    result = annotation.defaultShort();
                }
            }
        } else {
            result = Short.valueOf(nodeText);
        }

        return result;
    }

    private static byte getByte(JsonNode node, Method method) {
        byte result = (byte) 0;
        String nodeText = node.asText();
        if (node.isMissingNode() || node.isNull()) {
            if (method != null) {
                ConfigDescription annotation = method.getAnnotation(ConfigDescription.class);
                if (annotation != null) {
                    result = annotation.defaultByte();
                }
            }
        } else {
            result = Byte.valueOf(nodeText);
        }

        return result;
    }

    private static String getString(JsonNode node, Method method) {
        String result = "";
        if (node.isMissingNode() || node.isNull()) {
            if (method != null) {
                ConfigDescription annotation = method.getAnnotation(ConfigDescription.class);
                if (annotation != null) {
                    result = annotation.defaultString();
                }
            }
        } else {
            result = node.asText();
        }

        return result;
    }

    private Object getList(Type genericParameter, JsonNode value, Method method) throws IOException, InvalidConfigException {
        List<Object> list = new ArrayList<>();

        JsonNode currentValue = value;
        if (value.isMissingNode()) {
            ConfigDescription annotation = method.getAnnotation(ConfigDescription.class);
            if (annotation != null) {
                currentValue = yamlMapper.readTree("- " + String.join("\n- ", annotation.defaultList()));
            }
        }

        for (JsonNode node : currentValue) {
            list.add(nodeToObject(genericParameter, "", node, method));
        }

        return list;
    }

    private Object getInterface(Class<?> interfaceClass, String key, JsonNode node) {
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{interfaceClass},
                new ConfigProxyHandler(key, node, yamlMapper)
        );
    }

    private Object getMap(Type genericParameter, JsonNode value, Method method) throws InvalidConfigException, IOException {
        Map<String, Object> map = new HashMap<>();

        JsonNode currentValue = value;
        if (value.isMissingNode()) {
            ConfigDescription annotation = method.getAnnotation(ConfigDescription.class);
            if (annotation != null) {
                String mapStr = String.join("\n  ", annotation.defaultMap());
                currentValue = yamlMapper.readTree("key:\n  " + mapStr).at("/key");
            }
        }

        Iterator<Map.Entry<String, JsonNode>> fields = currentValue.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> node = fields.next();
            map.put(node.getKey(), nodeToObject(genericParameter, "", node.getValue(), method));
        }

        return map;
    }

    private Class<?> getClassDefinition(JsonNode value, Method method) throws ClassNotFoundException {
        String className = value.asText();
        if (value.isMissingNode() || className.isEmpty()) {
            ConfigDescription annotation = method.getAnnotation(ConfigDescription.class);
            if (annotation != null) {
                return annotation.defaultClass();
            } else {
                return null;
            }
        } else {
            return Class.forName(className);
        }
    }

    private Object getService(Class<?> serviceInterface, JsonNode value, Method method) {
        Class<?> serviceClass = serviceInterface;
        String serviceClassName = value.at("/$service").asText();
        if (!serviceClassName.isEmpty()) {
            try {
                serviceClass = Class.forName(serviceClassName);
            } catch (ClassNotFoundException ex) {
                LOGGER.warn("Invalid service class: " + serviceClassName);
            }
        }
        if (!serviceInterface.isAssignableFrom(serviceClass)) {
            LOGGER.warn("Invalid service class: " + serviceClassName + " it must implements: " + serviceInterface.getCanonicalName());
        }

        Object instance = null;
        Constructor<?> parametrizedConstructor = ClassInfo.getConstructorWithParameterImplementing(serviceClass, ServiceConnection.class);

        if (parametrizedConstructor == null) {
            try {
                Constructor<?> emptyConstructor = serviceClass.getConstructor();
                instance = emptyConstructor.newInstance();
            } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                LOGGER.error("Error while loading empty constructor serviceclass: " + serviceClass.getCanonicalName(), ex);
            }
        } else {
            ServiceDescription serviceDescription = method.getAnnotation(ServiceDescription.class);
            if (serviceDescription == null) {
                LOGGER.warn("Missing @ServiceDescription annotation for service: " + serviceClass.getCanonicalName());
            }

            Class<? extends ServiceConnection> connectionClass = null;
            String connectionClassName = value.at("/$connection").asText();
            if (connectionClassName.isEmpty()) {
                if (!serviceDescription.defaultConnection().equals(Class.class)) {
                    connectionClass = (Class<? extends ServiceConnection>) serviceDescription.defaultConnection();
                }
            } else {
                try {
                    connectionClass = (Class<? extends ServiceConnection>) Class.forName(connectionClassName);
                } catch (ClassNotFoundException ex) {
                    LOGGER.error("Can not found service connection class: " + connectionClassName, ex);
                }
            }

            if (connectionClass == null) {
                try {
                    Constructor<?> emptyConstructor = serviceClass.getConstructor();
                    instance = emptyConstructor.newInstance();
                } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    LOGGER.error("Error while loading empty constructor serviceclass: " + serviceClass.getCanonicalName(), ex);
                }
            } else {
                Class<?> connectionConfigClass = null;
                String connectionConfigClassName = value.at("/$connectionConfig").asText();

                if (connectionConfigClassName.isEmpty()) {
                    if (!serviceDescription.defaultConnectionConfig().equals(Class.class)) {
                        connectionConfigClass = serviceDescription.defaultConnectionConfig();
                    }
                } else {
                    try {
                        connectionConfigClass = Class.forName(connectionConfigClassName);
                    } catch (ClassNotFoundException ex) {
                        LOGGER.warn("Invalid service connection config class: " + serviceClassName);
                    }
                }

                ServiceConnection connectionInstance = null;
                if (connectionConfigClass != null) {
                    Constructor<? extends ServiceConnection> configurableConnectionConstructor = ClassInfo.getConstructorWithParameterImplementing(connectionClass, connectionConfigClass);

                    String configID = value.at("/$connectionConfigID").asText();
                    if (configID.isEmpty()) {
                        configID = serviceDescription.defaultConnectionConfigID();
                    }

                    try {
                        Object config = getInterface(connectionConfigClass, configID, value.at("/" + configID));

                        try {
                            connectionInstance = configurableConnectionConstructor.newInstance(config);
                        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                            LOGGER.error("Can load service connection with configuration: " + connectionClassName, ex);
                        }
                    } catch (Exception ex) {
                        LOGGER.error("Impossible to load service '" + configID + "' connection configuration: " + connectionConfigClass.getName(), ex);
                    }

                } else {
                    try {
                        Constructor<? extends ServiceConnection> simpleConnectionConstructor = connectionClass.getConstructor();
                        connectionInstance = simpleConnectionConstructor.newInstance();
                    } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                        LOGGER.error("Can load service connection without configuration: " + connectionClassName, ex);
                    }
                }

                if (connectionInstance != null) {
                    try {
                        instance = parametrizedConstructor.newInstance(connectionInstance);
                    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                        LOGGER.error("Error while loading constructor with connection for serviceclass: " + serviceClass.getCanonicalName(), ex);
                    }
                } else {
                    try {
                        Constructor<?> emptyConstructor = serviceClass.getConstructor();
                        instance = emptyConstructor.newInstance();
                    } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                        LOGGER.error("Error while loading empty constructor serviceclass: " + serviceClass.getCanonicalName(), ex);
                    }
                }
            }
        }

        return instance;
    }
}
