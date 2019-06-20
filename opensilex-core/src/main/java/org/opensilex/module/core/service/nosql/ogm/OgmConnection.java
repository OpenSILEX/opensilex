/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.service.nosql.ogm;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.spi.support.AbstractReadWriteAccess.Item;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.boot.OgmSessionFactoryBuilder;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.model.spi.AssociationOrderBy.Order;
import org.opensilex.module.core.service.nosql.NoSQLConnection;
import org.opensilex.module.core.service.nosql.exceptions.NoSQLTransactionException;

/**
 *
 * @author vincent
 */
public class OgmConnection implements NoSQLConnection {

    public OgmConnection(OgmConfig config) {
        StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder()
                .applySetting(OgmProperties.ENABLED, true)
                .applySetting(OgmProperties.DATASTORE_PROVIDER, config.provider());

        config.options().forEach((String key, String value) -> {
            registryBuilder.applySetting(key, value);
        });
        registry = registryBuilder.build();

        //build the SessionFactory
        sessionFactory = new MetadataSources(registry)
                .addAnnotatedClass(Order.class)
                .addAnnotatedClass(Item.class)
                .buildMetadata()
                .getSessionFactoryBuilder()
                .unwrap(OgmSessionFactoryBuilder.class)
                .build();

        transactionManager = com.arjuna.ats.jta.TransactionManager.transactionManager();
    }

    protected final StandardServiceRegistry registry;
    protected final OgmSessionFactory sessionFactory;
    protected final TransactionManager transactionManager;

    @Override
    public void startTransaction() throws NoSQLTransactionException {
        try {
            transactionManager.begin();
        } catch (NotSupportedException | SystemException ex) {
            throw new NoSQLTransactionException(ex);
        }
    }

    @Override
    public void commitTransaction() throws NoSQLTransactionException {
        try {
            transactionManager.commit();
        } catch (RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException | SystemException ex) {
            throw new NoSQLTransactionException(ex);
        }
    }

    @Override
    public void rollbackTransaction() throws NoSQLTransactionException {
        try {
            transactionManager.rollback();
        } catch (IllegalStateException | SecurityException | SystemException ex) {
            throw new NoSQLTransactionException(ex);
        }
    }
}
