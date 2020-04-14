/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.rest.cache;

import java.io.FileInputStream;
import java.util.Properties;
import org.opensilex.utils.ClassUtils;

/**
 *
 * @author vince
 */
public interface JCSApiCacheExtension {

    public default void configureJCSCache(Properties properties) throws Exception {
        properties.load(new FileInputStream(ClassUtils.getFileFromClassArtifact(this.getClass(), "jsc-cache.ccf")));
    }
;

}
