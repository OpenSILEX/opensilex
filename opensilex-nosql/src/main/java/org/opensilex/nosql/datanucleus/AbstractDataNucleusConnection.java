//******************************************************************************
//                   AbstractDataNucleusConnection.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.datanucleus;

import java.lang.invoke.MethodHandles;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Properties;
import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.jdo.JDOEnhancer;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.datanucleus.api.jdo.JDOPersistenceManagerFactory;
import org.datanucleus.metadata.PersistenceUnitMetaData;
import org.opensilex.nosql.NoSQLConfig;
import org.opensilex.nosql.exceptions.NoSQLTransactionException;
import org.opensilex.nosql.service.NoSQLConnection;
import org.opensilex.service.ServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Datanucleus connection implementation.
 * <pre>
 * TODO: Implement it
 * </pre>
 *
 * @see org.opensilex.nosql.service.NoSQLConnection
 * @author Vincent Migot
 */
public abstract class AbstractDataNucleusConnection implements NoSQLConnection {

    public final static Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    /**
     * static final for the JNDI name of the PersistenceManagerFactory
     */
    public static final String persistenceManagerFactoryName = "java:/datanucleus1";

    protected static PersistenceManagerFactory persistenceManagerFactory;
    protected final PersistenceUnitMetaData PMF_PROPERTIES;

    /**
     * Constructor for datanucleus allowing any Map of properties for
     * configuration depending of concrete implementation requirements.
     *
     * @param config
     */
    public AbstractDataNucleusConnection(ServiceConfig config) {
        PMF_PROPERTIES = this.getConfigProperties(config);
        PMF_PROPERTIES.addProperty("javax.jdo.option.ConnectionFactoryName", persistenceManagerFactoryName); 
        PMF_PROPERTIES.addProperty("datanucleus.PersistenceUnitName", "MyPersistenceUnit");

        LOGGER.debug(PMF_PROPERTIES.toString());
        persistenceManagerFactory = new JDOPersistenceManagerFactory(PMF_PROPERTIES, null);  
    }

    @Override
    public void startTransaction() throws NoSQLTransactionException {

    }

    @Override
    public void commitTransaction() throws NoSQLTransactionException {

    }

    @Override
    public void rollbackTransaction(Exception ex) throws Exception {
        if (ex != null) {
            throw ex;
        }
    }

    abstract protected PersistenceUnitMetaData getConfigProperties(ServiceConfig config);

    // convenience methods to get a PersistenceManager 

    /**
     * Method to get a PersistenceManager
     *
     * @return
     * @throws javax.naming.NamingException
     */
    public PersistenceManager getPersistenceManager()
            throws NamingException {
        return persistenceManagerFactory.getPersistenceManager();
    }

    public void closePersistanteManagerFactory() {
        persistenceManagerFactory.close();
    }
    // Now finally the bean method within a transaction
    public void testDataNucleusTrans()
            throws Exception {
        PersistenceManager pm = getPersistenceManager();
        try {
            // Do something with your PersistenceManager
        } finally {
            // close the PersistenceManager
            pm.close();
        }
    } 
    
}
