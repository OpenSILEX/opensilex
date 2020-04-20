//******************************************************************************
//                      MongoDBConnection.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.mongodb;

import org.opensilex.nosql.sample.FileModel;
import java.util.HashMap;
import java.util.Properties;
import javax.jdo.PersistenceManager;
import javax.naming.NamingException;
import org.datanucleus.metadata.PersistenceUnitMetaData;
import org.opensilex.OpenSilex;
import org.opensilex.nosql.NoSQLConfig;
import org.opensilex.nosql.datanucleus.AbstractDataNucleusConnection;
import org.opensilex.service.Service;
import org.opensilex.service.ServiceConfig;
import org.opensilex.service.ServiceConstructorArguments;
import org.opensilex.service.ServiceDefaultDefinition;

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

    private MongoDBConfig config;

    /**
     * Constructor for MongoDB connection
     * <pre>
     * TODO setup correct configuration
     * </pre>
     *
     * @param config MongoDB configuration
     */
    public MongoDBConnection(MongoDBConfig config) {
        super(config);
    }

    @Override
    public void startup() throws NamingException {
        try ( // TODO implement mongodb startup mechanism
            PersistenceManager persistenceManager = this.getPersistenceManager()) {
            persistenceManager.makePersistent(new FileModel("test"));
        }
    }

    @Override
    public void shutdown() {
        closePersistanteManagerFactory();
        // TODO implement mongodb shutdown mechanism
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
    protected PersistenceUnitMetaData getConfigProperties(ServiceConfig config) {
        MongoDBConfig mongoConfig = (MongoDBConfig) config;
        PersistenceUnitMetaData pumd = new PersistenceUnitMetaData("MyPersistenceUnit", "RESOURCE_LOCAL", null);

        pumd.addProperty("javax.jdo.option.ConnectionURL", "org.datanucleus.api.jdo.JDOPersistenceManagerFactory");
        pumd.addProperty("javax.jdo.option.ConnectionURL", "mongodb:" + mongoConfig.host() + ":" + mongoConfig.port() + "/" + mongoConfig.database());
        LOGGER.debug("javax.jdo.option.ConnectionURL" + "mongodb:" + mongoConfig.host() + ":" + mongoConfig.port() + "/" + mongoConfig.database());

        pumd.addProperty("javax.jdo.option.Mapping", "mongodb");
        pumd.addProperty("datanucleus.schema.autoCreateAll", "true"); 

        return pumd;
    }
}
