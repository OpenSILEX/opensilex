//******************************************************************************
//                   AbstractDataNucleusConnection.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.datanucleus;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import javax.jdo.JDOEnhancer;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.naming.NamingException;
import org.opensilex.nosql.NoSQLConfig;
import org.opensilex.nosql.service.NoSQLConnection;
import org.opensilex.service.ServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Datanucleus connection implementation.
 * <pre>
 * ObjectODO: Implement it
 * </pre>
 *
 * @see org.opensilex.nosql.service.NoSQLConnection
 * @author Vincent Migot, Arnaud Charleroy
 * @link https://cloud.google.com/appengine/docs/standard/java/datastore/jdo/creatinggettinganddeletingdata
 */
public abstract class AbstractDataNucleusConnection extends BaseService implements Service, NoSQLConnection {

    public final static Logger LOGGER = LoggerFactory.getLogger(AbstractDataNucleusConnection.class);
    /**
     * static final for the JNDI name of the PersistenceManagerFactory
     */
    protected static final String PMF_PERSISANT_UNIT_NAME = "myPersistenceUnit";
    protected static final String PMF_NAME = "MongoStore";

    protected PersistenceManagerFactory PMF;
    public Properties PMF_PROPERTIES;

    NoSQLConfig config;

    /**
     * static final for the JNDI name of the PersistenceManagerFactory
     */
//    protected static final String PMF_PERSISANT_UNIT_NAME = "myPersistenceUnit";
//    protected static final String PMF_NAME = "MongoStore";

    protected PersistenceManagerFactory PMF;

    protected Properties PMF_PROPERTIES;

    @Override
    public void setup() throws Exception {
        PMF_PROPERTIES = new Properties();
        PMF_PROPERTIES.setProperty("javax.jdo.PersistenceManagerFactoryClass", "org.datanucleus.api.jdo.JDOPersistenceManagerFactory");
    }

    @Override
    public void startup() throws Exception {
        PersistenceUnitMetaData pumd = new PersistenceUnitMetaData("dynamic-unit", "RESOURCE_LOCAL", null);
        pumd.setExcludeUnlistedClasses(false);
        PMF_PROPERTIES.forEach((key, value) -> pumd.addProperty((String) key, (String) value));

        PMF =  new JDOPersistenceManagerFactory(pumd, null);
    }

    // convenience methods to get a PersistenceManager 
    /**
     * Method to get a PersistenceManager
     *
     * @param config
     */
    public AbstractDataNucleusConnection(NoSQLConfig config) {
        this.config = config;
    }

    @Override
    public void setup() throws Exception {
        PMF_PROPERTIES = this.getConfigProperties(config);
        PMF_PROPERTIES.setProperty("javax.jdo.PersistenceManagerFactoryClass", "org.datanucleus.api.jdo.JDOPersistenceManagerFactory"); 
        LOGGER.debug(PMF_PROPERTIES.get("javax.jdo.option.ConnectionURL").toString());
        PMF = JDOHelper.getPersistenceManagerFactory(PMF_PROPERTIES);
        // NOT WORKING
//        JDOEnhancer enhancer = JDOHelper.getEnhancer();
//        enhancer.setVerbose(true);
//        enhancer.addPersistenceUnit(PMF.getPersistenceUnitName());
//        enhancer.enhance();
    }

    @Override
    public void startup() throws Exception {
    }

    /**
     *
     * @param config
     * @return
     */
    @Override
    abstract public Properties getConfigProperties(NoSQLConfig config);

    // convenience methods to get a PersistenceManager 
    /**
     * Method to get a PersistenceManager
     *
     * @return
     * @throws javax.naming.NamingException
     */
    @Override
    public PersistenceManager getPersistenceManager() throws NamingException {
        return PMF.getPersistenceManager();
    }

    @Override
    public Object create(Object instance) throws NamingException {
        return getPersistenceManager().makePersistent(instance);
    }

    @Override
    public void remove(Object instance) throws NamingException {
        getPersistenceManager().deletePersistent(instance);
    }

    @Override
    public Object findById(Class cls, Object key) throws NamingException {
        return getPersistenceManager().getObjectById(cls, key);
    }

    @Override
    public Collection find(Query query, Map parameters) throws NamingException {
        return (Collection) query.executeWithMap(parameters);
    }

    @Override
    public Object update(Object instance) throws NamingException {
        return getPersistenceManager().makePersistent(instance);
    }

    @Override
    public void createAll(Collection instances) throws NamingException {
        getPersistenceManager().makeTransactionalAll(instances);
    }

    @Override
    public void shutdown() {
        if (PMF == null) {
            PMF.close();
        }
    }

}
