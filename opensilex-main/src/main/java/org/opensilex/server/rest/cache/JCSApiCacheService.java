//******************************************************************************
//                              RestApplication.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.rest.cache;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.opensilex.service.BaseService;
import org.apache.commons.jcs.JCS;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache service implemtation for JSC.
 *
 * See: http://commons.apache.org/proper/commons-jcs/
 *
 * @author Vincent Migot
 */
public class JCSApiCacheService extends BaseService implements ApiCacheService {

    public JCSApiCacheService() {
        super(null);
    }

    /**
     * Class Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JCSApiCacheService.class);

    /**
     * Temprorary cache directory.
     */
    private File cacheDir;

    @Override
    public void startup() throws Exception {
        Properties properties = new Properties();

        cacheDir = Files.createTempDirectory("opensilex-temp").toFile();
        cacheDir.deleteOnExit();
        String cacheDirPath = cacheDir.getCanonicalPath();
        LOGGER.debug("Cache directory: " + cacheDirPath);

        List<JCSApiCacheExtension> cacheExtensions = getOpenSilex().getModulesImplementingInterface(JCSApiCacheExtension.class);
        for (JCSApiCacheExtension cacheExtension : cacheExtensions) {
            cacheExtension.configureJCSCache(properties);

            properties.forEach((key, value) -> {
                properties.put(key, value.toString().replaceAll("\\$\\{cache\\.dir\\}", cacheDirPath));
            });
        }

        if (LOGGER.isDebugEnabled()) {
            StringBuilder cacheProperties = new StringBuilder();
            properties.forEach((key, value) -> {
                cacheProperties.append("\n  " + key + ": " + value);
            });
            LOGGER.debug("Loaded cache configuration: " + cacheProperties);
        }

        JCS.setConfigProperties(properties);
    }

    @Override
    public void shutdown() throws Exception {
        FileUtils.cleanDirectory(cacheDir);
        JCS.shutdown();
    }

    @Override
    public boolean exists(String category, String key) {
        return JCS.getInstance(category).get(key) != null;
    }

    @Override
    public Object retrieve(String category, String key) {
        return JCS.getInstance(category).get(key);
    }

    @Override
    public void store(String category, String key, Object value) {
        JCS.getInstance(category).put(key, value);
    }

    @Override
    public void remove(String category, String key) {
        JCS.getInstance(category).remove(key);
    }

    @Override
    public Map<Object, Object> getMatching(String category, String keyPattern) {
        return JCS.getInstance(category).getMatching(keyPattern);
    }

    @Override
    public void remove(String category) {
        JCS.getInstance(category).clear();
    }

}
