package org.opensilex.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import org.opensilex.module.OpenSilexModule;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.opensilex.OpenSilex;
import static org.opensilex.OpenSilex.PROD_PROFILE_ID;
import org.opensilex.utils.ClassInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
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

        if (ClassInfo.isPrimitive(configClass)) {
            config = (T) ConfigProxyHandler.getPrimitive(key, root, null);
        } else {
            config = (T) Proxy.newProxyInstance(
                    Thread.currentThread().getContextClassLoader(),
                    new Class<?>[]{configClass},
                    new ConfigProxyHandler(key, root.deepCopy(), yamlMapper)
            );
        }
        return config;
    }

    public void toYaml(OutputStream os) throws IOException {
        yamlFactory.createGenerator(os).writeObject(root);
    }

    public void build(Path baseDirectory, Iterable<OpenSilexModule> modules, String id, File baseConfigFile) {
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

            for (OpenSilexModule module : modules) {
                addSource(module.getYamlFile(OpenSilex.PROD_PROFILE_ID));
                if (OpenSilex.DEV_PROFILE_ID.equals(extensionId)) {
                    addSource(module.getYamlFile(OpenSilex.DEV_PROFILE_ID));
                } else if (OpenSilex.TEST_PROFILE_ID.equals(extensionId)) {
                    addSource(module.getYamlFile(OpenSilex.TEST_PROFILE_ID));
                }
            }

            File mainConfig = baseDirectory.resolve("opensilex.yaml").toFile();
            if (mainConfig.exists() && mainConfig.isFile()) {
                addSource(mainConfig);
            }

            if (!extensionId.equals(PROD_PROFILE_ID)) {
                File idConfig = baseDirectory.resolve("opensilex-" + extensionId + ".yaml").toFile();
                if (idConfig.exists() && idConfig.isFile()) {
                    addSource(idConfig);
                }
            }

            if (buildExtension) {
                addSource(baseConfigFile);
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            toYaml(output);
            String yaml = new String(output.toByteArray(), StandardCharsets.UTF_8.name());
            LOGGER.debug("Loaded configuration");
            LOGGER.debug(yaml);
        } catch (IOException ex) {
            LOGGER.warn("Error while loading configuration", ex);
        }
    }

}
