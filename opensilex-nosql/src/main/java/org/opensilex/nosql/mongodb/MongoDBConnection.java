//******************************************************************************
//                      MongoDBConnection.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.mongodb;

import org.opensilex.nosql.datanucleus.AbstractDataNucleusConnection;
import org.opensilex.nosql.service.NoSQLConnection;
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
        configID = ""
)
public class MongoDBConnection extends AbstractDataNucleusConnection implements NoSQLConnection {

    public final static Logger LOGGER = LoggerFactory.getLogger(MongoDBConnection.class);

    private final MongoDBConfig config;

    public MongoDBConnection(MongoDBConfig config) {
        this.config = config;
    }

    @Override
    public void setup() throws Exception {
        super.setup();
        this.PMF_PROPERTIES.setProperty("javax.jdo.option.ConnectionURL", "mongodb:" + config.host() + ":" + config.port() + "/" + config.database());
        this.PMF_PROPERTIES.setProperty("javax.jdo.option.Mapping", "mongodb");
        this.PMF_PROPERTIES.setProperty("datanucleus.schema.autoCreateAll", "true");
    }

}
