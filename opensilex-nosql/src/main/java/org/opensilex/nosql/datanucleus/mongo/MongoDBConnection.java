//******************************************************************************
//                      MongoDBConnection.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.datanucleus.mongo;

import java.util.Properties;
import org.opensilex.nosql.datanucleus.DataNucleusService;
import org.opensilex.nosql.datanucleus.DataNucleusServiceConnection;
import org.opensilex.service.BaseService;
import org.opensilex.service.ServiceDefaultDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MongoDB connection for DataNucleus.
 * <pre>
 * TODO: Implement it
 * </pre>
 *
 * @see org.opensilex.nosql.datanucleus.DataNucleusService
 * @author Vincent Migot
 */
@ServiceDefaultDefinition(config = MongoDBConfig.class)
public class MongoDBConnection extends BaseService implements DataNucleusServiceConnection {

    public final static Logger LOGGER = LoggerFactory.getLogger(MongoDBConnection.class);

    public MongoDBConnection(MongoDBConfig config) {
        super(config);
    }

    public MongoDBConfig getImplementedConfig() {
        return (MongoDBConfig) this.getConfig();
    }

    private DataNucleusService datanucleus;

    @Override
    public void definePersistentManagerProperties(Properties pmfProperties) {
        MongoDBConfig cfg = getImplementedConfig();
        pmfProperties.setProperty("javax.jdo.option.ConnectionURL", "mongodb:" + cfg.host() + ":" + cfg.port() + "/" + cfg.database());
        pmfProperties.setProperty("javax.jdo.option.Mapping", "mongodb");
        pmfProperties.setProperty("datanucleus.schema.autoCreateAll", "true");
    }

    @Override
    public void setDatanucleus(DataNucleusService datanucleus) {
        this.datanucleus = datanucleus;
    }

}
