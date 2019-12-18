//******************************************************************************
//                      MongoDBConnection.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.mongodb;

import java.util.HashMap;
import org.opensilex.nosql.datanucleus.AbstractDataNucleusConnection;
import org.opensilex.service.ServiceConnection;

/**
 * MongoDB connection for DataNucleus.
 * <pre>
 * TODO: Implement it
 * </pre>
 *
 * @see org.opensilex.nosql.datanucleus.AbstractDataNucleusConnection
 * @author Vincent Migot
 */
public class MongoDBConnection extends AbstractDataNucleusConnection implements ServiceConnection {

    /**
     * Constructor for MongoDB connection
     * <pre>
     * TODO setup correct configuration
     * </pre>
     *
     * @param config
     */
    public MongoDBConnection(MongoDBConfig config) {
        // TODO setup correct configuration
        super(new HashMap<>());
    }

    @Override
    public void startup() {
        // TODO implement mongodb startup mechanism
    }

    @Override
    public void shutdown() {
        // TODO implement mongodb shutdown mechanism
    }

}
