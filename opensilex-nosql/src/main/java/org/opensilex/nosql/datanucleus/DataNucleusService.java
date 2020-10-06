//******************************************************************************
//                   DataNucleusService.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.datanucleus;

import com.mongodb.client.MongoClient;
import com.mongodb.DuplicateKeyException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import javax.jdo.JDOHelper;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.JDOQLTypedQuery;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.jdo.query.BooleanExpression;
import javax.naming.NamingException;
import org.datanucleus.PropertyNames;
import org.datanucleus.api.jdo.JDOPersistenceManagerFactory;
import org.datanucleus.api.jdo.JDOQueryCache;
import org.datanucleus.metadata.PersistenceUnitMetaData;
import org.opensilex.OpenSilex;
import org.opensilex.nosql.exceptions.NoSQLAlreadyExistingUriException;
import org.opensilex.nosql.exceptions.NoSQLBadPersistenceManagerException;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.exceptions.NoSQLTooLargeSetException;
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
        //pumd.addClassName("org.opensilex.core.provenance.dal.ProvenanceModel");
        PMF_PROPERTIES.forEach((key, value) -> pumd.addProperty((String) key, (String) value));
        Map<String, Object> props = new HashMap<>();
        props.put(PropertyNames.PROPERTY_CLASSLOADER_PRIMARY, OpenSilex.getClassLoader());
        PMF = new JDOPersistenceManagerFactory(pumd, props);

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

    /**
     * Method to generate a valid URI for the instance
     *
     * @param instance will be updated by a new generated URI
     * @param checkUriExist necessary to check if URI already existing
     * @throws Exception
     */
    private <T extends NoSQLModel> void generateUniqueUriIfNullOrValidateCurrent(T instance, List<URI> alreadyGenerateURI) throws Exception {
        URI uri = instance.getUri();

        if (uri == null) {

            int retry = 0;
            String graphPrefix = baseURI.resolve(instance.getGraphPrefix()).toString();
            uri = instance.generateURI(graphPrefix, instance, retry);
            while (alreadyGenerateURI.contains(uri)) {
                uri = instance.generateURI(graphPrefix, instance, retry++);
            }

            instance.setUri(uri);
       } else if (alreadyGenerateURI.contains(uri)) {
            throw new NoSQLAlreadyExistingUriException(uri);
        }
    }
    
    public <T extends NoSQLModel> void generateUniqueUriIfNullOrValidateCurrent(T instance) throws Exception {
        generateUniqueUriIfNullOrValidateCurrent(instance, new ArrayList<>() );
    }
    
     /**
      * Method to prepare an instance for creation
      * 
      * @param instance will be created
      * @return DBMS Id of instance
      * @throws Exception 
      */
    public <T extends NoSQLModel> Object prepareInstanceCreation(T instance) throws Exception{
        return prepareInstanceCreation(instance, false);
    }
    
    public <T extends NoSQLModel> Object prepareInstanceCreation(T instance, boolean forceURI) throws Exception{
        generateUniqueUriIfNullOrValidateCurrent(instance);
        if (instance.getUri() == null){
            generateUniqueUriIfNullOrValidateCurrent(instance);
            return createForceURI(instance);
        }else{
            return create(instance);
        }
    }
    
    public <T extends NoSQLModel> void prepareInstancesListCreation(Collection<T> instances) throws Exception {
        List<URI> alreadyGenerateURI = new ArrayList<>();
        List<T> instancesUserURI = new ArrayList<>();
        List<T> instancesGeneratedURI = new ArrayList<>();
        if(instances.size() > 10000)
            throw new NoSQLTooLargeSetException();
        
        for (T instance:instances){
            if(instance.getUri()!= null)
                instancesUserURI.add(instance);
            else{
                generateUniqueUriIfNullOrValidateCurrent(instance, alreadyGenerateURI);
                instancesGeneratedURI.add(instance);
            }
            alreadyGenerateURI.add(instance.getUri());
        }
        
        try{
            createAllForceURI(instancesGeneratedURI);
            createAllNoSQLModel(instancesUserURI);
        }catch (Exception e){
            throw e;
        }
    }
    
    private <T extends NoSQLModel> T createTransactionnal(Transaction tx1, PersistenceManager persistenceManager, T instance) throws Exception{
        String graphPrefix = baseURI.resolve(instance.getGraphPrefix()).toString();
        int retry = 0;
        URI uri = null;
        int errorCode = 11000;
        T insertInstance = null;

        while(errorCode == 11000){
            try{
                //TimeUnit.SECONDS.sleep(1);
                insertInstance = (T) persistenceManager.makePersistent(instance);
                errorCode = 0;
            }catch(Exception err){
                Throwable cause = err.getCause();

                if(cause instanceof DuplicateKeyException){
                    errorCode = ((DuplicateKeyException) cause).getCode();
                }else{
                    tx1.setRollbackOnly();
                    throw err;
                }

                uri = instance.generateURI(graphPrefix, instance, retry++);
                try{
                    instance.setUri(uri);
                }catch(Exception e){
                    tx1.setRollbackOnly();
                    throw e;
                }
            }
        }
        
        return insertInstance;
    }
    
    public <T extends NoSQLModel> Object prepareInstanceCreation(T instance, boolean forceURI) throws Exception{
        generateUniqueUriIfNullOrValidateCurrent(instance);
        if (forceURI)
            return createForceURI(instance);
        else
            return create(instance);
    }
    
    public <T extends NoSQLModel> void prepareInstancesListCreation(Collection<T> instances) throws Exception {
        List<URI> alreadyGenerateURI = new ArrayList<>();
        List<T> instancesUserURI = new ArrayList<>();
        List<T> instancesGeneratedURI = new ArrayList<>();
        for (T instance:instances){
            if(instance.getUri()!= null)
                instancesUserURI.add(instance);
            else{
                generateUniqueUriIfNullOrValidateCurrent(instance, alreadyGenerateURI);
                instancesGeneratedURI.add(instance);
            }
            alreadyGenerateURI.add(instance.getUri());
        }
        
        try{
            createAllForceURI(instancesGeneratedURI);
            createAll(instancesUserURI);
        }catch (Exception e){
            throw e;
        }
    }
    
    private <T extends NoSQLModel> T createTransactionnal(Transaction tx1, PersistenceManager persistenceManager, T instance) throws Exception{
        String graphPrefix = baseURI.resolve(instance.getGraphPrefix()).toString();
        int retry = 0;
        URI uri = null;
        int errorCode = 11000;
        T insertInstance = null;

        while(errorCode == 11000){
            try{
                insertInstance = (T) persistenceManager.makePersistent(instance);
                errorCode = 0;
            }catch(Exception err){
                Throwable cause = err.getCause();

                if(cause instanceof DuplicateKeyException){
                    errorCode = ((DuplicateKeyException) cause).getCode();
                }else{
                    tx1.setRollbackOnly();
                    throw new NoSQLDuplicateKeyException(String.valueOf(retry));
                }

                uri = instance.generateURI(graphPrefix, instance, retry++);
                try{
                    instance.setUri(uri);
                }catch(Exception e){
                    tx1.setRollbackOnly();
                    throw e;
                }
            }
        }
        
        return insertInstance;
    }

    /**
     * Method to create a new data
     *
     * @param instance will be created
     * @return DBMS Id of instance
     * @throws NamingException
     */
    @Override
    public Object create(Object instance) throws NamingException {
       
       try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
           Transaction tx1 = persistenceManager.currentTransaction();
           Object insertInstance = null;
            try{
                tx1.setRestoreValues(true);
                tx1.begin();
                insertInstance = persistenceManager.makePersistent(instance);
                tx1.commit();
                return JDOHelper.getObjectId(instance);
            }catch (Exception ex){
                if(tx1.isActive()) tx1.rollback();
                throw ex;
            }finally{
                persistenceManager.close();
            }
                            

        }
    }
    
    public <T extends NoSQLModel> Object createForceURI(T instance) throws NamingException, NoSQLBadPersistenceManagerException, Exception {
       
        try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
            Transaction tx1 = persistenceManager.currentTransaction();
            Object insertInstance = null;
            try{
                tx1.begin();
            try{
                tx1.setRestoreValues(true);
                tx1.begin();
                insertInstance = persistenceManager.makePersistent(instance);
                return JDOHelper.getObjectId(instance);
            }catch (Exception ex){
                tx1.rollback();
                return null;
            }finally{
                persistenceManager.close();
            }
                            

        }
    }
    
    public <T extends NoSQLModel> Object createForceURI(T instance) throws NamingException, NoSQLBadPersistenceManagerException {
       
        try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
            Transaction tx1 = persistenceManager.currentTransaction();
            Object insertInstance = null;
            try{
                tx1.begin();
                insertInstance = createTransactionnal(tx1, persistenceManager,(NoSQLModel) instance);
                tx1.commit();
                return JDOHelper.getObjectId(instance);
            } catch (Exception ex) {
                java.util.logging.Logger.getLogger(DataNucleusService.class.getName()).log(Level.SEVERE, null, ex);
                if(tx1.isActive()) tx1.rollback();
                throw ex;
            }finally{
                persistenceManager.close();
            }
        }
    }
    
    @Override
    public void createAll(Collection instances) throws NamingException{
        try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
            Transaction tx1 = persistenceManager.currentTransaction();
            List<Object> insertInstances = new ArrayList<>();
            try{
                tx1.begin();
                for(Object instance: instances){
                    Object insertInstance = persistenceManager.makePersistent(instance);
                    insertInstances.add(insertInstance);
                }
                tx1.commit();
            } catch (Exception ex) {
                java.util.logging.Logger.getLogger(DataNucleusService.class.getName()).log(Level.SEVERE, null, ex);
                if(tx1.isActive()) tx1.rollback();
                deleteAll(insertInstances);
                throw ex;
            }finally{
                persistenceManager.close();
            }
        }
    }
    
    public <T extends NoSQLModel>void createAllNoSQLModel(Collection<T> instances) throws NamingException, NoSQLInvalidURIException, NoSQLBadPersistenceManagerException{
        try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
            Transaction tx1 = persistenceManager.currentTransaction();
            List<T> insertInstances = new ArrayList<>();
            try{
                tx1.begin();
                for(T instance: instances){
                    T insertInstance = persistenceManager.makePersistent(instance);
                    insertInstances.add(insertInstance);
                }
                tx1.commit();
            } catch (Exception ex) {
                java.util.logging.Logger.getLogger(DataNucleusService.class.getName()).log(Level.SEVERE, null, ex);
                if(tx1.isActive()) tx1.rollback();
                deleteList(insertInstances);
                throw ex;
            }finally{
                persistenceManager.close();
            }
        }
    }
    
<<<<<<< HEAD
    public <T extends NoSQLModel> void createAllForceURI(Collection<T> instances) throws NamingException, Exception{
        try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
            Transaction tx1 = persistenceManager.currentTransaction();
            List<T> insertInstances = new ArrayList<>();
            try{
                tx1.begin();
                for(T instance: instances){
                    T insertInstance = createTransactionnal(tx1, persistenceManager, instance);
                    insertInstances.add(insertInstance);
                }
                tx1.commit();
            } catch (Exception ex) {
                java.util.logging.Logger.getLogger(DataNucleusService.class.getName()).log(Level.SEVERE, null, ex);
                deleteList(insertInstances);
                throw ex;
            }finally{
                persistenceManager.close();
            }
        }
    }
    
=======
>>>>>>> Size max for set of data + datanucleus rollback
    public <T extends NoSQLModel> void createAllForceURI(Collection<T> instances) throws NamingException, Exception{
        try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
            Transaction tx1 = persistenceManager.currentTransaction();
            List<T> insertInstances = new ArrayList<>();
            try{
                tx1.begin();
                for(T instance: instances){
                    T insertInstance = createTransactionnal(tx1, persistenceManager, instance);
                    insertInstances.add(insertInstance);
                }
                tx1.commit();
            } catch (Exception ex) {
                java.util.logging.Logger.getLogger(DataNucleusService.class.getName()).log(Level.SEVERE, null, ex);
                deleteList(insertInstances);
                throw ex;
            }finally{
                persistenceManager.close();
            }
        }
    }
    
    public <T extends NoSQLModel> void createAllForceURI(Collection<T> instances) throws NamingException, Exception{
        try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
            Transaction tx1 = persistenceManager.currentTransaction();
            List<T> insertInstances = new ArrayList<>();
            try{
                tx1.begin();
                for(T instance: instances){
                    T insertInstance = createTransactionnal(tx1, persistenceManager, instance);
                    insertInstances.add(insertInstance);
                }
                tx1.commit();
            } catch (Exception ex) {
                java.util.logging.Logger.getLogger(DataNucleusService.class.getName()).log(Level.SEVERE, null, ex);
                deleteList(insertInstances);
                throw ex;
            }finally{
                persistenceManager.close();
            }
        }
    }
    
    /**
     * Method to update data in database
     * 
     * @param instance the new data with an already existing URI in the database
     * @throws NamingException
     * @throws NoSQLInvalidURIException
     * @throws NoSQLBadPersistenceManagerException 
     */
    public <T extends NoSQLModel> void update(T instance) throws NamingException, NoSQLInvalidURIException, NoSQLBadPersistenceManagerException{
        try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
            try{
                T oldInstance = findByURI(instance, instance.getUri(),persistenceManager);
                if (oldInstance == null)
                    throw new NoSQLInvalidURIException(instance.getUri());

                T newInstance = oldInstance.update(instance);
                delete((Object)oldInstance, persistenceManager);
                update((Object) newInstance,persistenceManager);
            }finally{
                persistenceManager.close();
            }
        } 
    }
    
    /**
     * Method to write update data 
     * 
     * @param instance update data
     * @param pm Persistence Manager
     * @return DBMS Id
     * @throws NamingException
     * @throws NoSQLBadPersistenceManagerException 
     */
    public Object update(Object instance, PersistenceManager pm) throws NamingException, NoSQLBadPersistenceManagerException {
        if(pm == null) throw new NoSQLBadPersistenceManagerException();
        
        Transaction tx1 = pm.currentTransaction();
        tx1.begin();
        pm.makePersistent(instance);
        tx1.commit();
        return JDOHelper.getObjectId(instance);
    }
    
    /**
     * Method to write update data 
     * 
     * @param instance update data
     * @return DBMS Id
     * @throws NamingException
     */
    @Override
    public Object update(Object instance) throws NamingException {
        return create(instance);
    }
    
    /**
     * Method to update data in database
     * 
     * @param instance the new data with an already existing URI in the database
     * @throws NamingException
     * @throws NoSQLInvalidURIException
     * @throws NoSQLBadPersistenceManagerException 
     */
    public <T extends NoSQLModel> void update(T instance) throws NamingException, NoSQLInvalidURIException, NoSQLBadPersistenceManagerException{
        try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
            try{
                T oldInstance = findByURI(instance, instance.getUri(),persistenceManager);
                if (oldInstance == null)
                    throw new NoSQLInvalidURIException(instance.getUri());

                T newInstance = oldInstance.update(instance);
                delete((Object)oldInstance, persistenceManager);
                update((Object) newInstance,persistenceManager);
            }finally{
                persistenceManager.close();
            }
        } 
    }
    
    /**
     * Method to write update data 
     * 
     * @param instance update data
     * @param pm Persistence Manager
     * @return DBMS Id
     * @throws NamingException
     * @throws NoSQLBadPersistenceManagerException 
     */
    public Object update(Object instance, PersistenceManager pm) throws NamingException, NoSQLBadPersistenceManagerException {
        if(pm == null) throw new NoSQLBadPersistenceManagerException();
        
        Transaction tx1 = pm.currentTransaction();
        tx1.begin();
        pm.makePersistent(instance);
        tx1.commit();
        return JDOHelper.getObjectId(instance);
    }
    
    /**
     * Method to write update data 
     * 
     * @param instance update data
     * @return DBMS Id
     * @throws NamingException
     */
    @Override
    public Object update(Object instance) throws NamingException {
        return create(instance);
    }
    
    /**
     * Method to update data in database
     * 
     * @param instance the new data with an already existing URI in the database
     * @throws NamingException
     * @throws NoSQLInvalidURIException
     * @throws NoSQLBadPersistenceManagerException 
     */
    public <T extends NoSQLModel> void update(T instance) throws NamingException, NoSQLInvalidURIException, NoSQLBadPersistenceManagerException{
        try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
            try{
                T oldInstance = findByURI(instance, instance.getUri(),persistenceManager);
                if (oldInstance == null)
                    throw new NoSQLInvalidURIException(instance.getUri());

                T newInstance = oldInstance.update(instance);
                delete((Object)oldInstance, persistenceManager);
                update((Object) newInstance,persistenceManager);
            }finally{
                persistenceManager.close();
            }
        } 
    }
    
    /**
     * Method to write update data 
     * 
     * @param instance update data
     * @param pm Persistence Manager
     * @return DBMS Id
     * @throws NamingException
     * @throws NoSQLBadPersistenceManagerException 
     */
    public Object update(Object instance, PersistenceManager pm) throws NamingException, NoSQLBadPersistenceManagerException {
        if(pm == null) throw new NoSQLBadPersistenceManagerException();
        
        Transaction tx1 = pm.currentTransaction();
        tx1.begin();
        pm.makePersistent(instance);
        tx1.commit();
        return JDOHelper.getObjectId(instance);
    }
    
    /**
     * Method to write update data 
     * 
     * @param instance update data
     * @return DBMS Id
     * @throws NamingException
     */
    @Override
    public Object update(Object instance) throws NamingException {
        return create(instance);
    }
    
    /**
     * Method to delete already founded instance 
     * with an already existing PersistenceManager
     *
     * @param instance document to be deleted
     * @param pm PersistenceManager which has opened instance
     * @throws NamingException
     * @throws NoSQLBadPersistenceManagerException
     */
    public void delete(Object instance, PersistenceManager pm) throws NamingException, NoSQLBadPersistenceManagerException{
        if (pm == null) throw new NoSQLBadPersistenceManagerException();

        if (instance != null) {
            Transaction transaction = pm.currentTransaction();
            transaction.begin();
            pm.deletePersistent(instance);
            transaction.commit();
        }
    }

    /**
     * Method to find and delete instance from URI
     *
     * @param instance of class implements NoSQLModel, can't be null but can be unused model
     * @param uri document URI
     * @throws NamingException
     * @throws NoSQLInvalidURIException
     * @throws NoSQLBadPersistenceManagerException
     */
    public <T extends NoSQLModel> void delete(T instance, URI uri) throws NamingException, NoSQLInvalidURIException, NoSQLBadPersistenceManagerException{
        try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
            try{
                T oldInstance = findByURI(instance, uri, persistenceManager);
                if (oldInstance == null)
                    throw new NoSQLInvalidURIException(instance.getUri());
                delete((Object)oldInstance, persistenceManager);
            }finally{
                persistenceManager.close();
            }
        }
    }

    /**
     * Method to find and delete instance from DBMS key
     *
     * @param cls
     * @param key DBMS Id
     * @throws NamingException
     */
    @Override
    public void delete(Class cls, Object key) throws NamingException {
        try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
            Object foundedObject = persistenceManager.getObjectById(cls, key);

            if (foundedObject != null) {
                try{
                    Transaction transaction = persistenceManager.currentTransaction();
                    transaction.begin();
                    persistenceManager.deletePersistent(foundedObject);
                    transaction.commit();
                }finally {
                    persistenceManager.close();
                }
            }
        }
    }
    
    public <T extends NoSQLModel> void deleteList(Collection<T> instances) throws NamingException, NoSQLInvalidURIException, NoSQLBadPersistenceManagerException{
        for(T instance: instances){
            try{
                delete(instance, instance.getUri());
            }catch(NoSQLInvalidURIException ex){
                continue;
            }
        }
    }
    
    public <T extends NoSQLModel> void deleteList(Collection<T> instances) throws NamingException, NoSQLInvalidURIException, NoSQLBadPersistenceManagerException{
        for(T instance: instances){
            try{
                delete(instance, instance.getUri());
            }catch(NoSQLInvalidURIException ex){
                continue;
            }
        }
    }
    @Override
    public void deleteAll(Collection instances) throws NamingException {
        try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
            try{
                persistenceManager.deletePersistentAll(instances);
            }finally{
                persistenceManager.close();
            }
        }
    }
    
    public void deleteAll(Collection instances,PersistenceManager pm) throws NamingException, NoSQLBadPersistenceManagerException {
        if (pm == null) throw new NoSQLBadPersistenceManagerException();
        Transaction transaction = pm.currentTransaction();
        transaction.begin();
        pm.deletePersistentAll(instances);
        transaction.commit();
    }

    @Override
    public Long deleteAll(JDOQLTypedQuery query) throws NamingException {
        return (Long) query.deletePersistentAll();

    }
      
    /**
     * Method to find data by DBMS Id
     *
     * @param cls
     * @param key DBMS Id
     * @return instance of cls
     * @throws NamingException
     */
    @Override
    public <T> T findById(Class cls, Object key) throws NamingException {
        try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
            try {
                return (T) persistenceManager.getObjectById(cls, key);
            } catch (JDOObjectNotFoundException e) {
                return null;
            }finally{
                persistenceManager.close();
            }
        }
    }

    /**
     * Method to find data by it URI
     *
     * @param instance of class implements NoSQLModel, can't be null but can be unused model
     * @param uri of reuired data
     * @return <T extends NoSQLModel> required data
     * @throws NamingException
     * @throws NoSQLInvalidURIException
     */
    public <T extends NoSQLModel> T findByURI(T instance, URI uri) throws  NoSQLInvalidURIException, NamingException{
        try (PersistenceManager persistenceManager = getPersistentConnectionManager()) {
            try{
                BooleanExpression expr = null;
                JDOQLTypedQuery<T> tq = persistenceManager.newJDOQLTypedQuery((Class<T>) instance.getClass());
                expr = instance.getURIExpr(uri);
                String exp = expr.toString();
                T result = null;
                if (expr != null){
                    result = tq.filter(expr).executeUnique();
                }else{
                    throw new NoSQLInvalidURIException(uri);
                }
                return result;
            }finally{
                persistenceManager.close();
            }
        }
    }

    /**
     * Method to find data by it URI
     *
     * @param instance of class implements NoSQLModel, can't be null but can be unused model
     * @param uri of reuired data
     * @param PM Persistence Manager used to find and open required data
     * @return <T extends NoSQLModel> required data
     * @throws NamingException
     * @throws NoSQLInvalidURIException
     * @throws NoSQLBadPersistenceManagerException
     */
    public <T extends NoSQLModel> T findByURI(T instance, URI uri, PersistenceManager PM) throws NamingException, NoSQLInvalidURIException, NoSQLBadPersistenceManagerException{
        if (PM == null) throw new NoSQLBadPersistenceManagerException();

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
    }

    /**
     * Method to test for the existence of URI in he database
     *
     * @param instance of class implements NoSQLModel, can't be null but can be unused model
     * @param uri tested
     * @return boolean, True = uri exist
     * @throws NoSQLInvalidURIException
     * @throws NamingException
     */
    public <T extends NoSQLModel> boolean existByURI(T instance, URI uri) throws NoSQLInvalidURIException, NamingException{
        T result = findByURI(instance,uri);
        return result!=null;
    }
    
    public <T extends NoSQLModel> List<T> searchWithPagination(
            PersistenceManager pm,
            Class<T> objectClass,
            String filter,
            Map params,
            Integer page,
            Integer pageSize,
            int total) throws Exception {

 
        List<T> results;
        if (pageSize == null || pageSize == 0) {
            results = new ArrayList<>();
        } else if (total > 0 && (page * pageSize) < total) {
            Integer offset = null;
            Integer limit = null;
            if (page == null || page < 0) {
                page = 0;
            }
            if (pageSize != null && pageSize > 0) {
                offset = page * pageSize;
                limit = pageSize;
            }
            Query selectQuery = pm.newQuery(objectClass);
            selectQuery.setFilter(filter);
            selectQuery.setNamedParameters(params);
            selectQuery.setRange(offset, offset+limit);
            results = selectQuery.executeList();
        } else {
            results = new ArrayList<>();
        }

        return results;

    }

    public <T extends Object & NoSQLModel> int countResults(PersistenceManager pm, Class<T> objectClass, String filter, Map params) throws NamingException {
        Query countQuery = pm.newQuery("SELECT count(uri) FROM " + objectClass.getName());
        countQuery.setFilter(filter);
        Object countResult = countQuery.executeWithMap(params);
        int total = (int) (long) countResult;
        return total;
    }

    @Override
    public MongoClient getMongoDBClient() {
        return this.connection.getMongoDBClient();
    }

     @Override
    public Long count(JDOQLTypedQuery query) throws NamingException {
        return (Long) query.executeResultUnique();
    }
    
    /**
     * Erase JDO Cache
     */
    public void flush(){
        JDOQueryCache cache = ((JDOPersistenceManagerFactory)PMF).getQueryCache();
        cache.evictAll();
    }
    
    @Override
    public void shutdown() {
        if (PMF != null) {
            PMF.close();
        }
    }
}
