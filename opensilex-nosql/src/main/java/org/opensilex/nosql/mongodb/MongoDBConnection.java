//******************************************************************************
//                      MongoDBConnection.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.mongodb;

import java.util.Properties;
import javax.naming.NamingException;
import org.opensilex.OpenSilex;
import org.opensilex.nosql.NoSQLConfig;
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
        configClass = MongoDBConfig.class
)
public class MongoDBConnection extends AbstractDataNucleusConnection implements Service {

    public final static Logger LOGGER = LoggerFactory.getLogger(MongoDBConnection.class);

    /**
     * Constructor for MongoDB connection
     * <pre>
     * TODO setup correct configuration
     * </pre>
     *
     * @param config MongoDB configuration
     * @throws javax.naming.NamingException
     */
    public MongoDBConnection(NoSQLConfig config) throws NamingException {
        super(config);
    }

    private ServiceConstructorArguments constructorArgs;

    @Override
    public void setServiceConstructorArguments(ServiceConstructorArguments args) {
        this.constructorArgs = args;
    }

    @Override
    public ServiceConstructorArguments getServiceConstructorArguments() {
        return this.constructorArgs;
    }

    private OpenSilex opensilex;

    @Override
    public void setOpenSilex(OpenSilex opensilex) {
        this.opensilex = opensilex;
    }

    @Override
    public OpenSilex getOpenSilex() {
        return this.opensilex;
    }

    @Override
    public Properties getConfigProperties(NoSQLConfig config) {
        MongoDBConfig mongoConfig = (MongoDBConfig) config;
        Properties props = new Properties();
        props.setProperty("javax.jdo.option.ConnectionURL", "mongodb:" + mongoConfig.host() + ":" + mongoConfig.port() + "/" + mongoConfig.database());
        props.setProperty("javax.jdo.option.Mapping", "mongodb");
        props.setProperty("datanucleus.schema.autoCreateAll", "true");

        return props;
    }

    @Override
    public void startup() {
    }

}
