//******************************************************************************
//                      MongoDBConnection.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.mongodb;

import java.util.HashMap;
import org.opensilex.OpenSilex;
import org.opensilex.nosql.datanucleus.AbstractDataNucleusConnection;
import org.opensilex.service.Service;
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

    /**
     * Constructor for MongoDB connection
     * <pre>
     * TODO setup correct configuration
     * </pre>
     *
     * @param config MongoDB configuration
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
}
