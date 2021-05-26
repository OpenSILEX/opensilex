/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.elastic.service;

import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.opensilex.elastic.ElasticConfig;
import org.opensilex.elastic.ElasticServerConfig;
import org.opensilex.service.BaseService;

/**
 *
 * @author vmigot
 */
public class ElasticService extends BaseService {

    public final static String DEFAULT_ELASTIC_SERVICE = "elastic";

    private final ElasticConfig cfg;

    public ElasticService(ElasticConfig cfg) {
        super(cfg);
        this.cfg = (ElasticConfig) getConfig();
    }

    public RestHighLevelClient getClient() throws Exception {
        if (!cfg.enableElasticSearch()) {
            throw new Exception("Elastic search feature is disabled !");
        }

        if (cfg.servers().size() == 0) {
            throw new Exception("No elastic search server defined in configuration !");
        }

        List<HttpHost> hosts = new ArrayList<>();

        for (ElasticServerConfig server : cfg.servers()) {
            HttpHost host = new HttpHost(server.host(), server.port(), "http");
            hosts.add(host);
        }

        RestHighLevelClient elasticClient = new RestHighLevelClient(
                RestClient.builder(hosts.toArray(new HttpHost[hosts.size()]))
        );

        return elasticClient;
    }
}
