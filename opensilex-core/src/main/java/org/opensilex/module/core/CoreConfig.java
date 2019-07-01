//******************************************************************************
//                      ApplicationCoreConfig.java
// OpenSILEX
// Copyright © INRA 2019
// Creation date: 01 jan. 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.module.core;

import java.util.Map;
import org.opensilex.config.ConfigDescription;
import org.opensilex.module.ModuleConfig;
import org.opensilex.service.ServiceConfig;

/**
 * Core configuration interface
 */
public interface CoreConfig extends ModuleConfig {

    /**
     * Flag to determine if application is in debug mode or not
     *
     * @return true Application in debug mode false Application in production
     * mode
     */
    @ConfigDescription(
            value = "Flag to determine if application is in debug mode or not",
            defaultBoolean = false
    )
    Boolean debug();

    /**
     * Default application language
     *
     * @return default application language
     */
    @ConfigDescription(
            value = "Default application language",
            defaultString = "en"
    )
    String defaultLanguage();

    @ConfigDescription(
            value = "Core data sources",
            defaultMap = {
                "sparql: {"
                    + "serviceClass: org.opensilex.service.sparql.SPARQLService,"
                    + "connectionClass: org.opensilex.service.sparql.rdf4j.RDF4JConnection,"
                    + "configId: opensilex-core-service-sparql,"
                    + "configClass: org.opensilex.module.core.service.sparql.rdf4j"
                + "}",
                "nosql: {"
                    + "serviceClass: org.opensilex.service.nosql.NoSQLService,"
                    + "connectionClass: org.opensilex.service.nosql.ogm.OgmConnection,"
                    + "configId: opensilex-core-service-nosql,"
                    + "configClass: org.opensilex.module.core.service.nosql.ogm"
                + "}",
                "fs: {"
                    + "serviceClass: org.opensilex.service.fs.FileStorageService,"
                    + "connectionClass: org.opensilex.service.fs.local.LocalFileSystemConnection,"
                    + "configId: opensilex-core-service-fs,"                     
                    + "configClass: org.opensilex.module.core.service.fs.local.LocalFileSystemConfig"
                + "}"              
            }
    )
    Map<String, ServiceConfig> services();

}
