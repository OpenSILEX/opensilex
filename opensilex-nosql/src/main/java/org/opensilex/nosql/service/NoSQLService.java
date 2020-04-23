//******************************************************************************
//                         NoSQLService.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.service;

import java.util.Collection;
import java.util.Map;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.naming.NamingException;
import org.opensilex.nosql.mongodb.MongoDBConnection;
import org.opensilex.service.BaseService;
import org.opensilex.service.Service;
import org.opensilex.service.ServiceDefaultDefinition;

/**
 * Service for big data access and storage.
 * <pre>
 * TODO: Only implement transaction for the moment, datanucleus integration
 * to achieve: http://www.datanucleus.org/
 * </pre>
 *
 * @author Vincent Migot
 */
@ServiceDefaultDefinition(
        serviceClass = MongoDBConnection.class,
        serviceID = "mongodb"
)
public class NoSQLService extends BaseService implements NoSQLConnection, Service {

    public final static String DEFAULT_NOSQL_SERVICE = "sparql";
    private final NoSQLConnection connection;

    /**
     * Constructor with NoSQLConnection to allow multiple configurable
     * implementations
     *
     * @param connection Connection for the service
     */
    public NoSQLService(NoSQLConnection connection) {
        this.connection = connection;
    }

    @Override
    public void setup() throws Exception {
        connection.setOpenSilex(getOpenSilex());
        connection.setup();
    }

    @Override
    public void startup() throws Exception {
        connection.startup();
    }

    @Override
    public void shutdown() throws Exception {
        connection.shutdown();
    }

    @Override
    public Object create(Object instance) throws NamingException {
        return this.connection.create(instance);
    }

    @Override
    public void delete(Object instance) throws NamingException {
        this.connection.delete(instance);
    }

    @Override
    public Object findById(Class cls, Object key) throws NamingException {
        return this.connection.findById(cls, key);
    }

    @Override
    public Collection find(Query query, Map parameters) throws NamingException {
        return this.connection.find(query, parameters);
    }

    @Override
    public Object update(Object instance) throws NamingException {
        return this.connection.update(instance);
    }

    @Override
    public void createAll(Collection instances) throws NamingException {
        this.connection.createAll(instances);
    }

    @Override
    public PersistenceManager getPersistenceManager() throws NamingException {
        return this.connection.getPersistenceManager();
    }

}
