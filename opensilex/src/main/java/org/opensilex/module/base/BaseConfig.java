/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.base;

import java.util.Map;
import org.opensilex.config.ConfigDescription;
import org.opensilex.module.ModuleConfig;
import org.opensilex.service.ServiceConfig;

/**
 *
 * @author vincent
 */
public interface BaseConfig extends ModuleConfig {

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
    public Boolean debug();

    /**
     * Default application language
     *
     * @return default application language
     */
    @ConfigDescription(
            value = "Default application language",
            defaultString = "en"
    )
    public String defaultLanguage();

    @ConfigDescription(
            value = "Base data sources",
            defaultMap = {
                "sparql: {"
                + "serviceClass: org.opensilex.sparql.SPARQLService,"
                + "connectionClass: org.opensilex.sparql.rdf4j.RDF4JConnection,"
                + "configId: opensilex-service-rdf4j,"
                + "configClass: org.opensilex.sparql.rdf4j.RDF4JConfig"
                + "}",
                "authentication: {"
                + "serviceClass: org.opensilex.server.security.AuthenticationService"
                + "}",
                "fs: {"
                + "serviceClass: org.opensilex.fs.FileStorageService"
                + "}",
                "bigdata: {"
                + "serviceClass: org.opensilex.bigdata.BigDataService,"
                + "connectionClass: org.opensilex.bigdata.mongodb.MongoDBConnection,"
                + "configId: opensilex-service-mongodb,"
                + "configClass: org.opensilex.bigdata.mongodb.MongoDBConfig"   
                + "}"
            }
    )
    public Map<String, ServiceConfig> services();

}
