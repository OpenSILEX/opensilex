/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.base;

import org.opensilex.bigdata.BigDataConnection;
import org.opensilex.bigdata.BigDataService;
import org.opensilex.bigdata.mongodb.MongoDBConfig;
import org.opensilex.bigdata.mongodb.MongoDBConnection;
import org.opensilex.config.ConfigDescription;
import org.opensilex.config.ServiceDescription;
import org.opensilex.fs.FileStorageService;
import org.opensilex.module.ModuleConfig;
import org.opensilex.server.security.AuthenticationService;
import org.opensilex.sparql.SPARQLConnection;
import org.opensilex.sparql.SPARQLService;
import org.opensilex.sparql.rdf4j.RDF4JConfig;
import org.opensilex.sparql.rdf4j.RDF4JConnection;

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

    @ServiceDescription(
            value = "Big data source",
            connection = BigDataConnection.class,
            defaultConnection = MongoDBConnection.class,
            defaultConnectionConfig = MongoDBConfig.class,
            defaultConnectionConfigID = "mongodb"
    )
    public BigDataService bigData();

    @ServiceDescription(
            value = "SPARQL data sources",
            connection = SPARQLConnection.class,
            defaultConnection = RDF4JConnection.class,
            defaultConnectionConfig = RDF4JConfig.class,
            defaultConnectionConfigID = "rdf4j"
    )
    public SPARQLService sparql();

    @ServiceDescription(
            value = "Authentication service"
    )
    public AuthenticationService authentication();

    @ServiceDescription(
            value = "File storage service"
    )
    public FileStorageService fs();

}
