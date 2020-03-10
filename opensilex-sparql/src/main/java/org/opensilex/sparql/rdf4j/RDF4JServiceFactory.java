/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.rdf4j;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.pool.PoolStats;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vince
 */
public class RDF4JServiceFactory extends SPARQLServiceFactory {

    private final static Logger LOGGER = LoggerFactory.getLogger(RDF4JServiceFactory.class);

    private final Repository repository;
    private PoolingHttpClientConnectionManager cm;

    public RDF4JServiceFactory(RDF4JConfig config) {
        LOGGER.debug("Build RDF4JServiceFactory from config");
        synchronized (this) {
            HTTPRepository repo = new HTTPRepository(config.serverURI(), config.repository());
            cm = new PoolingHttpClientConnectionManager();
            cm.setDefaultMaxPerRoute(20);
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setConnectionManager(cm)
                    .build();

            repo.setHttpClient(httpClient);
            repo.init();
            this.repository = repo;
        }

    }

    public RDF4JServiceFactory(Repository repository) {
        LOGGER.debug("Build RDF4JServiceFactory from repository");
        synchronized (this) {
            this.repository = repository;
            this.repository.init();
        }
    }

    private synchronized SPARQLService getNewService() throws Exception {
        RepositoryConnection connection = repository.getConnection();
        if (cm != null && LOGGER.isDebugEnabled()) {
            PoolStats stats = cm.getTotalStats();
            LOGGER.debug(
                    "Connection pool stats: \n" +
                    "In use    -> " + stats.getLeased()+ "\n" +
                    "Pending   -> " + stats.getPending()+ "\n" +
                    "Available -> " + stats.getAvailable() + "\n" +
                    "Max       -> " + stats.getMax()+ "\n"
            );
        }
        SPARQLService sparql = new SPARQLService(new RDF4JConnection(connection));
        sparql.startup();

        return sparql;
    }

    private synchronized void closeService(SPARQLService sparql) throws Exception {
        sparql.shutdown();
    }

    @Override
    public SPARQLService provide() {
        try {
            return getNewService();
        } catch (Exception ex) {
            LOGGER.error("Error while opening RDF4J service connectioninstance", ex);
            return null;
        }
    }

    @Override
    public void dispose(SPARQLService sparql) {
        try {
            closeService(sparql);
        } catch (Exception ex) {
            LOGGER.error("Error while closing RDF4J service connectioninstance instance", ex);
        }

    }
}
