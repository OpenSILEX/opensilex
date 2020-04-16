//******************************************************************************
//                              RestApplication.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.rest.cache;

import java.io.FileInputStream;
import java.util.Properties;
import org.opensilex.utils.ClassUtils;

/**
 * Cache extension for JSC.
 *
 * @author Vincent Migot
 */
public interface JCSApiCacheExtension {

    /**
     * JSC cache configuration, default implementation load "jsc-cache.ccf" file in resources.
     *
     * @param properties Global JSC cache properties.
     * @throws Exception
     */
    public default void configureJCSCache(Properties properties) throws Exception {
        properties.load(new FileInputStream(ClassUtils.getFileFromClassArtifact(this.getClass(), "jsc-cache.ccf")));
    }
}
