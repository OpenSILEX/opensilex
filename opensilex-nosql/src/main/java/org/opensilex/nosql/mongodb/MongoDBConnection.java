//******************************************************************************
//                      MongoDBConnection.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.mongodb;

import java.util.Properties;
import javax.naming.NamingException;
import org.codehaus.janino.Mod;
import org.opensilex.OpenSilex;
import org.opensilex.nosql.NoSQLConfig;
import org.opensilex.nosql.NoSQLDBConfig;
import org.opensilex.nosql.datanucleus.AbstractDataNucleusConnection;
import org.opensilex.service.Service;
import org.opensilex.service.ServiceConfig;
import org.opensilex.service.ServiceConstructorArguments;
import org.opensilex.service.ServiceDefaultDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MongoDB connection for DataNucleus.
 * <pre>
 * TODO: Implement it
 * </pre>
 *
 * @see org.opensilex.nosql.datanucleus.AbstractDataNucleusConnection
 * @author Vincent Migot
 */
@ServiceDefaultDefinition(
        configClass = MongoDBConfig.class,
        configID = "mongodb"
)
public class MongoDBConnection extends AbstractDataNucleusConnection {

    public final static Logger LOGGER = LoggerFactory.getLogger(MongoDBConnection.class);
    
    private static MongoDBConfig customMongoConfig = null;

    public MongoDBConnection() { }

    public MongoDBConnection(MongoDBConfig config) {
        this.customMongoConfig = config;
    }

    @Override
    public void setup() throws Exception {
        if(customMongoConfig == null){
            LOGGER.warn("Loading default config");
            config = getOpenSilex().loadConfigPath("big-data.nosql.mongodb", MongoDBConfig.class);
        }else{
            LOGGER.warn("Loading custom config");
            config = customMongoConfig;
        }
       
        super.setup();
        MongoDBConfig mongoConfig = (MongoDBConfig) config;
        System.out.println("org.opensilex.nosql.mongodb.MongoDBConnection.setup()");
        this.PMF_PROPERTIES.setProperty("javax.jdo.option.ConnectionURL", "mongodb:" + mongoConfig.host() + ":" + mongoConfig.port() + "/" + mongoConfig.database());
        this.PMF_PROPERTIES.setProperty("javax.jdo.option.Mapping", "mongodb");
        this.PMF_PROPERTIES.setProperty("datanucleus.schema.autoCreateAll", "true");
    }

}
