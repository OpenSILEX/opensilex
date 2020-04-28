//******************************************************************************
//                   AbstractDataNucleusConnection.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.datanucleus;

import java.util.Collection;
import java.util.List;
import java.util.Properties;
import javax.jdo.JDOEnhancer;
import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.JDOQLTypedQuery;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.naming.NamingException;
import org.datanucleus.api.jdo.JDOPersistenceManagerFactory;
import org.datanucleus.metadata.PersistenceUnitMetaData;
import org.opensilex.nosql.service.NoSQLConnection;
import org.opensilex.service.BaseService;
import org.opensilex.service.Service;
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
     * @return
     * @throws javax.naming.NamingException
     */
    @Override
    public PersistenceManager getPersistentConnectionManager() throws NamingException {
        return PMF.getPersistenceManager();
    }

    @Override
    public Object create(Object instance) throws NamingException {
        try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
            Transaction tx1 = persistenceManager.currentTransaction();
            tx1.begin();
            persistenceManager.makePersistent(instance);
            tx1.commit();
            return JDOHelper.getObjectId(instance);
        }
    }

    @Override
    public void delete(Class cls, Object key) throws NamingException {
        Object foundedObject  = findById(cls, key);

        if (foundedObject != null) {
            try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
                Transaction transaction = persistenceManager.currentTransaction();
                persistenceManager.deletePersistent(foundedObject);
                transaction.commit();
            }
        }
    }
    
  


    @Override
    public <T> T findById(Class cls, Object key) throws NamingException {
        try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
            try {
                return (T) persistenceManager.getObjectById(cls, key);
            } catch (JDOObjectNotFoundException e) {
                return null;
            }
        }
    }

    @Override
    public Long count(JDOQLTypedQuery query) throws NamingException {
        return (Long) query.executeResultUnique();
    }

     @Override
    public Object update(Object instance) throws NamingException {
        return  create(instance);
    }

    @Override
    public void createAll(Collection instances) throws NamingException {
        try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
            persistenceManager.makeTransactionalAll(instances);
        }
    }

    @Override
    public void deleteAll(Collection instances) throws NamingException {
        try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
            persistenceManager.deletePersistentAll(instances);
        }
    }

    @Override
    public Long deleteAll(JDOQLTypedQuery query) throws NamingException {
        return (Long) query.deletePersistentAll();

    }

    @Override
    public void shutdown() {
        if (PMF == null) {
            PMF.close();
        }
    }

}
