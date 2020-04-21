//******************************************************************************
//                        NoSQLConnection.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.service;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.naming.NamingException;
import org.opensilex.nosql.NoSQLConfig;
import org.opensilex.service.Service;

/**
 * Interface to describe big data connection required features.
 * <pre>
 * ObjectODO: Only implement transaction for the moment, datanucleus integration
 * to achieve: http://www.datanucleus.org/
 * </pre>
 *
 * @see org.opensilex.nosql.service.NoSQLService
 * @author Vincent Migot
 */
public interface NoSQLConnection<Object> extends Service {

    Properties getConfigProperties(NoSQLConfig config);

    public Object create(Object instance) throws NamingException;

    public void remove(Object instance) throws NamingException;

    public Object findById(Class cls, Object key) throws NamingException;

    public Collection find(Query<Object> query, Map parameters) throws NamingException;

    public Object update(Object instance) throws NamingException;

    public void createAll(Collection<Object> instances) throws NamingException;

    public PersistenceManager getPersistenceManager()throws NamingException;
}
