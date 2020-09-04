//******************************************************************************
//                   DataNucleusService.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.datanucleus;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.JDOQLTypedQuery;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;
import javax.jdo.query.BooleanExpression;
import javax.naming.NamingException;
import org.datanucleus.PropertyNames;
import org.datanucleus.api.jdo.JDOPersistenceManagerFactory;
import org.datanucleus.metadata.PersistenceUnitMetaData;
import org.opensilex.OpenSilex;
import org.opensilex.nosql.exceptions.NoSQLAlreadyExistingUriException;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.model.NoSQLModel;
import org.opensilex.nosql.service.NoSQLService;
import org.opensilex.service.BaseService;
import org.opensilex.service.ServiceDefaultDefinition;
import org.opensilex.sparql.SPARQLModule;
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
@ServiceDefaultDefinition(config = DataNucleusServiceConfig.class)
public class DataNucleusService extends BaseService implements NoSQLService {

    public final static Logger LOGGER = LoggerFactory.getLogger(DataNucleusService.class);
    /**
     * static final for the JNDI name of the PersistenceManagerFactory
     */
//    protected static final String PMF_PERSISANT_UNIT_NAME = "myPersistenceUnit";
//    protected static final String PMF_NAME = "MongoStore";

    protected PersistenceManagerFactory PMF;

    protected Properties PMF_PROPERTIES;
    private final DataNucleusServiceConnection connection;
    
    protected PersistenceManager PM;

    private URI baseURI;
    
    public DataNucleusService(DataNucleusServiceConfig config) {
        super(config);
        this.connection = config.connection();
    }

    @Override
    public void setup() throws Exception {
        PMF_PROPERTIES = new Properties();
        PMF_PROPERTIES.setProperty("javax.jdo.PersistenceManagerFactoryClass", "org.datanucleus.api.jdo.JDOPersistenceManagerFactory");
        this.connection.definePersistentManagerProperties(PMF_PROPERTIES);
        SPARQLModule sparqlModule = this.getOpenSilex().getModuleByClass(SPARQLModule.class);
        baseURI = sparqlModule.getBaseURI();
    }

    @Override
    public void startup() throws Exception {
        PersistenceUnitMetaData pumd = new PersistenceUnitMetaData("dynamic-unit", "RESOURCE_LOCAL", null);
        pumd.setExcludeUnlistedClasses(false);
        PMF_PROPERTIES.forEach((key, value) -> pumd.addProperty((String) key, (String) value));
        Map<String, Object> props = new HashMap<>();
        props.put(PropertyNames.PROPERTY_CLASSLOADER_PRIMARY, OpenSilex.getClassLoader());
        PMF = new JDOPersistenceManagerFactory(pumd, props);
        PM = getPersistentConnectionManager();
        
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

    /*public <T extends NoSQLModel> Object prepareAndCreate(T instance) throws Exception{
        generateUniqueUriIfNullOrValidateCurrent(instance, true);
        return create(instance);
    }*/
    
    public <T extends NoSQLModel> Object prepareInstanceCreation(T instance) throws Exception{
        generateUniqueUriIfNullOrValidateCurrent(instance, true);
        return create((Object)instance);
    }
    
    @Override
    public Object create(Object instance) throws NamingException {
       
       // try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
            
            Transaction tx1 = PM.currentTransaction();
            tx1.begin();
            PM.makePersistent(instance);
            tx1.commit();
            return JDOHelper.getObjectId(instance);
       // }
    }
    
    private <T extends NoSQLModel> void generateUniqueUriIfNullOrValidateCurrent(T instance, boolean checkUriExist) throws Exception {
        URI uri = instance.getUri();
         
        if (uri == null) {
 
            int retry = 0;
            String graphPrefix = baseURI.resolve(instance.getGraphPrefix()).toString();
            uri = instance.generateURI(graphPrefix, instance, retry);
            while (findByUri(instance, uri) != null) {
                uri = instance.generateURI(graphPrefix, instance, retry++);
            }

            instance.setUri(uri);
       } else if (checkUriExist && (findByUri(instance, uri) != null)) {
            throw new NoSQLAlreadyExistingUriException(uri);
        }
    }

    @Override
    public void delete(Class cls, Object key) throws NamingException {
        Object foundedObject = findById(cls, key);

        if (foundedObject != null) {
            //try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
                Transaction transaction = PM.currentTransaction();
                transaction.begin();
                PM.deletePersistent(foundedObject);
                transaction.commit();
            //}
        }
    }
    
    public void delete(Object instance) throws NamingException{
        if (instance != null) {
            //try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
                Transaction transaction = PM.currentTransaction();
                transaction.begin();
                PM.deletePersistent(instance);
                transaction.commit();
            //}
        }
    }

    @Override
    public <T> T findById(Class cls, Object key) throws NamingException {
        //try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
            try {
                return (T) PM.getObjectById(cls, key);
            } catch (JDOObjectNotFoundException e) {
                return null;
            }
        //}
    }

    public <T extends NoSQLModel> T findByUri(T instance, URI uri) throws NamingException, NoSQLInvalidURIException{
        //try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
            BooleanExpression expr = null;
            JDOQLTypedQuery<T> tq = PM.newJDOQLTypedQuery((Class<T>) instance.getClass());
            expr = instance.getURIExpr(uri);
            T result = null;
            if (expr != null){
                result = tq.filter(expr).executeUnique();
            }else{
                throw new NoSQLInvalidURIException(uri);
            }
            return result;
        //}
    }
    
    @Override
    public Long count(JDOQLTypedQuery query) throws NamingException {
        return (Long) query.executeResultUnique();
    }

    public <T extends NoSQLModel> void update_(T instance) throws NamingException, NoSQLInvalidURIException{
        T oldInstance = findByUri(instance, instance.getUri());
        if (oldInstance == null)
            throw new NoSQLInvalidURIException(instance.getUri());
        
        T newInstance = oldInstance.update(instance);
        delete((Object)oldInstance);
        update((Object) newInstance);
    }
            
    @Override
    public Object update(Object instance) throws NamingException {
        return create(instance);
    }

    @Override
    public void createAll(Collection instances) throws NamingException {
        //try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
            PM.makeTransactionalAll(instances);
        //}
    }

    @Override
    public void deleteAll(Collection instances) throws NamingException {
        //try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
            PM.deletePersistentAll(instances);
        //}
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
