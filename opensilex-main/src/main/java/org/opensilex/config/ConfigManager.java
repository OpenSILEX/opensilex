//******************************************************************************
//                          ConfigManager.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import net.logstash.logback.encoder.org.apache.commons.lang3.StringUtils;
import org.opensilex.OpenSilex;
import static org.opensilex.OpenSilex.PROD_PROFILE_ID;
import org.opensilex.OpenSilexConfig;
import org.opensilex.OpenSilexModule;
import org.opensilex.service.Service;
import org.opensilex.service.ServiceConfig;
import org.opensilex.service.ServiceConstructorArguments;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <pre>
 * Configuration manager based on YAML files
 * TODO update Javadoc
 * </pre>
 *
 * @author Vincent Migot
 */
public class ConfigManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private ObjectNode root = mapper.createObjectNode();
    private final YAMLFactory yamlFactory = new YAMLFactory();
    private final ObjectMapper yamlMapper = new ObjectMapper(yamlFactory);

    public ConfigManager() {
        yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public void addSource(String yamlString) throws IOException {
        addSource(yamlMapper.readTree(yamlString));
    }

    public void addLines(String... yamlLines) throws IOException {
        addSource(yamlMapper.readTree(String.join("\n", yamlLines)));
    }

    public void addSource(InputStream yamlInput) throws IOException {
        addSource(yamlMapper.readTree(yamlInput));
    }

    public void addSource(File yamlFile) throws IOException {
        addSource(yamlMapper.readTree(yamlFile));
    }

    public void addSource(JsonNode tree) throws IOException {
        root = mapper.updateValue(root, tree);
    }

    @SuppressWarnings("unchecked")
    public <T> T loadConfig(String key, Class<T> configClass) {
        T config;

        if (ClassUtils.isPrimitive(configClass)) {
            config = (T) ConfigProxyHandler.getPrimitive(key, root, null);
        } else {
            config = (T) Proxy.newProxyInstance(
                    OpenSilex.getClassLoader(),
                    new Class<?>[]{configClass},
                    new ConfigProxyHandler(key, root.deepCopy(), yamlMapper)
            );
        }
        return config;
    }

    @SuppressWarnings("unchecked")
    public <T> T loadConfigPath(String path, Class<T> configClass) {

        JsonNode baseNode = root;
        String finalKey = path;

        String[] pathParts = path.split("\\.");
        if (pathParts.length > 0) {
            String jsonPointer = "";

            for (int i = 0; i < pathParts.length - 1; i++) {
                jsonPointer += "/" + pathParts[i];
            }
            baseNode = root.at(jsonPointer);
            finalKey = pathParts[pathParts.length - 1];
        }

        T config;

        if (ClassUtils.isPrimitive(configClass)) {
            config = (T) ConfigProxyHandler.getPrimitive(configClass.getCanonicalName(), baseNode.at("/" + finalKey), null);
        } else {
            config = (T) Proxy.newProxyInstance(
                    OpenSilex.getClassLoader(),
                    new Class<?>[]{configClass},
                    new ConfigProxyHandler(finalKey, baseNode.deepCopy(), yamlMapper)
            );
        }
        return config;
    }

    public void toYaml(OutputStream os) throws IOException {
        yamlFactory.createGenerator(os).writeObject(root);
    }

    public OpenSilexConfig buildSystemConfig(File baseConfigFile) throws IOException {
        ObjectMapper systemMapper = new ObjectMapper(yamlFactory);
        ObjectNode systemRoot = systemMapper.createObjectNode();

        if (baseConfigFile != null) {
            if (baseConfigFile.exists() && baseConfigFile.isFile()) {
                systemRoot = systemMapper.updateValue(systemRoot, systemMapper.readTree(baseConfigFile));
            }
        }

        return (OpenSilexConfig) Proxy.newProxyInstance(
                OpenSilex.getClassLoader(),
                new Class<?>[]{OpenSilexConfig.class},
                new ConfigProxyHandler(OpenSilexConfig.YAML_KEY, systemRoot.deepCopy(), systemMapper)
        );
    }

    public void build(Path baseDirectory, Iterable<OpenSilexModule> modules, String id, File baseConfigFile, OpenSilexConfig systemConfig) throws IOException {
        try {
            String extensionId = id;
            boolean buildExtension = false;
            if (baseConfigFile != null) {
                if (baseConfigFile.exists() && baseConfigFile.isFile()) {
                    addSource(baseConfigFile);
                    extensionId = loadConfig("extend", String.class);
                    buildExtension = true;
                    if (!OpenSilex.PROD_PROFILE_ID.equals(extensionId)
                            && !OpenSilex.DEV_PROFILE_ID.equals(extensionId)
                            && !OpenSilex.TEST_PROFILE_ID.equals(extensionId)) {
                        extensionId = OpenSilex.PROD_PROFILE_ID;
                    }
                }
            }

            Map<String, String> ignoredModules = systemConfig.ignoredModules();
            for (OpenSilexModule module : modules) {
                if (ignoredModules.containsValue(module.getClass().getCanonicalName())) {
                    continue;
                }

                InputStream prodFile = module.getConfigFile(OpenSilex.PROD_PROFILE_ID);
                if (prodFile != null) {
                    addSource(prodFile);
                }

                if (OpenSilex.DEV_PROFILE_ID.equals(extensionId)) {
                    InputStream devFile = module.getConfigFile(OpenSilex.DEV_PROFILE_ID);
                    if (devFile != null) {
                        addSource(devFile);
                    }
                } else if (OpenSilex.TEST_PROFILE_ID.equals(extensionId)) {
                    InputStream testFile = module.getConfigFile(OpenSilex.TEST_PROFILE_ID);
                    if (testFile != null) {
                        addSource(testFile);
                    }
                }
            }

            File mainConfig = baseDirectory.resolve("opensilex.yml").toFile();
            if (mainConfig.exists() && mainConfig.isFile()) {
                addSource(mainConfig);
            }

            if (!extensionId.equals(PROD_PROFILE_ID)) {
                File idConfig = baseDirectory.resolve("opensilex-" + extensionId + ".yml").toFile();
                if (idConfig.exists() && idConfig.isFile()) {
                    addSource(idConfig);
                }
            }

            if (buildExtension) {
                addSource(baseConfigFile);
            }

            if (LOGGER.isDebugEnabled()) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                toYaml(output);
                String yaml = new String(output.toByteArray(), StandardCharsets.UTF_8.name());
                LOGGER.debug("Loaded configuration");
                LOGGER.debug(yaml);
            }

        } catch (IOException ex) {
            LOGGER.warn("Error while loading configuration", ex);
            throw ex;
        }

    }

    public String getExpandedYAMLConfig(OpenSilexConfig systemConfig, Iterable<OpenSilexModule> modules) throws Exception {
        StringBuilder yml = new StringBuilder();

        yml.append("\n# " + StringUtils.repeat("-", 78) + "\n");
        yml.append("# Base system configuration " + OpenSilex.class.getSimpleName() + " (" + OpenSilexConfig.class.getSimpleName() + ")\n");
        addConfigInterface(yml, "system", systemConfig, OpenSilexConfig.class, 0);

        for (OpenSilexModule module : modules) {
            String configId = module.getConfigId();
            Class<?> configClass = module.getConfigClass();
            if (configId != null && configClass != null && !configId.isEmpty()) {
                yml.append("\n# " + StringUtils.repeat("-", 78) + "\n");
                yml.append("# Configuration for module: " + module.getClass().getSimpleName() + " (" + configClass.getSimpleName() + ")\n");
                Object config = module.getConfig();
                addConfigInterface(yml, configId, config, configClass, 0);
            }
        };

        return yml.toString();
    }

    private final static String YAML_SEPARATOR = "  ";

    private void addConfigInterface(StringBuilder yml, String key, Object value, Class<?> objectClass, int depth) throws Exception {
        int elementDepth = depth;
        if (key != null && !key.isEmpty()) {
            yml.append(StringUtils.repeat(YAML_SEPARATOR, depth) + key + ":\n");
            elementDepth = depth + 1;
        }
        for (Method method : objectClass.getMethods()) {
            ConfigDescription cfgDescription = method.getAnnotation(ConfigDescription.class);
            if (cfgDescription != null) {
                Object itemValue = method.invoke(value);
                addConfigPropertyValue(yml, method.getName(), itemValue, method.getGenericReturnType(), cfgDescription, elementDepth);
            }
        }
    }

    private void addConfigPropertyValue(StringBuilder yml, String key, Object value, Type returnType, ConfigDescription cfgDescription, int depth) throws Exception {
        if (cfgDescription != null) {
            yml.append(StringUtils.repeat(YAML_SEPARATOR, depth) + "# " + cfgDescription.value() + " (" + getShortType(returnType) + ")\n");
        }

        if (ClassUtils.isGenericType(returnType)) {
            ParameterizedType genericReturnType = (ParameterizedType) returnType;
            Type genericParameter = genericReturnType.getActualTypeArguments()[0];

            Class<?> returnTypeClass = (Class<?>) genericReturnType.getRawType();
            if (ClassUtils.isList(returnTypeClass)) {
                if (key != null) {
                    yml.append(StringUtils.repeat(YAML_SEPARATOR, depth) + key + ":\n");
                }
                Type genericValueParameter = genericReturnType.getActualTypeArguments()[0];
                List<?> list = (List<?>) value;
                for (Object item : list) {
                    addConfigPropertyListValue(yml, item, genericValueParameter, depth + 1);
                }
            } else if (ClassUtils.isMap(returnTypeClass)) {
                Type genericValueParameter = genericReturnType.getActualTypeArguments()[1];
                if (key != null) {
                    yml.append(StringUtils.repeat(YAML_SEPARATOR, depth) + key + ":\n");
                }
                Map<String, ?> map = (Map<String, ?>) value;
                for (Map.Entry<String, ?> entry : map.entrySet()) {
                    addConfigPropertyValue(yml, entry.getKey(), entry.getValue(), genericValueParameter, null, depth + 1);
                }
            } else if (ClassUtils.isClass(returnTypeClass)) {
                yml.append(StringUtils.repeat(YAML_SEPARATOR, depth) + ((Class<?>) value).getCanonicalName() + "\n");
            } else {
                LOGGER.warn("Unexpected generic parameter for configuration: " + key + " - " + returnTypeClass.getSimpleName() + "<" + genericParameter.getTypeName() + ">");
            }
        } else {
            Class<?> returnTypeClass = (Class<?>) returnType;
            if (ClassUtils.isPrimitive(returnTypeClass)) {
                if (key != null) {
                    yml.append(StringUtils.repeat(YAML_SEPARATOR, depth) + key + ": " + value.toString() + "\n");
                } else {
                    yml.append(value.toString() + "\n");
                }
            } else if (Service.class.isAssignableFrom(returnTypeClass)) {
                if (key != null) {
                    yml.append(StringUtils.repeat(YAML_SEPARATOR, depth) + key + ":\n");
                }
                addConfigService(yml, key, (Service) value, returnType, depth + 1);
            } else if (ClassUtils.isInterface(returnTypeClass)) {
                addConfigInterface(yml, key, value, returnTypeClass, depth);
            } else {
                LOGGER.warn("Unexpected parameter for configuration: " + key + " - " + returnTypeClass.getCanonicalName());
            }
        }
    }

    private void addConfigPropertyListValue(StringBuilder yml, Object value, Type type, int depth) throws Exception {
        yml.append(StringUtils.repeat(YAML_SEPARATOR, depth) + "- ");
        addConfigPropertyValue(yml, null, value, type, null, depth + 1);
    }

    private void addConfigService(StringBuilder yml, String name, Service service, Type serviceBaseType, int depth) throws Exception {
        ServiceConstructorArguments serviceConfig = service.getServiceConstructorArguments();
        yml.append(StringUtils.repeat(YAML_SEPARATOR, depth) + "# Service implementation class for: " + name + " (" + getShortType(serviceBaseType) + ")\n");
        yml.append(StringUtils.repeat(YAML_SEPARATOR, depth) + "implementation: " + serviceConfig.implementation().getCanonicalName() + "\n");
        if (serviceConfig.configID() != null && serviceConfig.configClass() != null) {
            Constructor<?> constructor = ClassUtils.getConstructorWithParameterImplementing(serviceConfig.implementation(), ServiceConfig.class);
            yml.append(StringUtils.repeat(YAML_SEPARATOR, depth) + "# Configuration class used as constructor parameter for: " + name + " (" + getShortType(constructor.getParameters()[0].getType()) + ")\n");
            yml.append(StringUtils.repeat(YAML_SEPARATOR, depth) + "configClass: " + serviceConfig.configClass().getCanonicalName() + "\n");
            if (!serviceConfig.configID().isEmpty()) {
                yml.append(StringUtils.repeat(YAML_SEPARATOR, depth) + "# Configuration identifier used as constructor parameter for: " + name + "\n");
                yml.append(StringUtils.repeat(YAML_SEPARATOR, depth) + "configID: " + serviceConfig.configID() + "\n");
            }
            addConfigInterface(yml, serviceConfig.configID(), serviceConfig.getConfig(), serviceConfig.configClass(), depth);

        } else if (serviceConfig.getService() != null) {
            yml.append(StringUtils.repeat(YAML_SEPARATOR, depth) + "# Service class used as constructor parameter for: " + name + "\n");
            yml.append(StringUtils.repeat(YAML_SEPARATOR, depth) + "serviceClass: " + serviceConfig.serviceClass().getCanonicalName() + "\n");

            String serviceID = serviceConfig.serviceID();
            if (serviceID != null && !serviceID.isEmpty()) {
                yml.append(StringUtils.repeat(YAML_SEPARATOR, depth) + "# Service identifier used as constructor parameter for: " + name + "\n");
                yml.append(StringUtils.repeat(YAML_SEPARATOR, depth) + "serviceID: " + serviceID + "\n");
                yml.append(StringUtils.repeat(YAML_SEPARATOR, depth) + serviceID + ":\n");

                Constructor<? extends Service> constructor = ClassUtils.getConstructorWithParameterImplementing(serviceConfig.implementation(), Service.class);
                addConfigService(yml, serviceID, serviceConfig.getService(), constructor.getParameters()[0].getType(), depth + 1);
            }
        }
    }

    private static String getShortType(Type returnType) {
        return returnType.getTypeName().replaceAll("([^.<]+\\.)+", "");
    }
}
