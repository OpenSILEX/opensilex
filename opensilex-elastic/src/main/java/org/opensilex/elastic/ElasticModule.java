/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.elastic;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.opensilex.OpenSilexModule;
import org.opensilex.elastic.service.ElasticService;
import org.opensilex.server.extensions.APIExtension;

/**
 *
 * @author vmigot
 */
public class ElasticModule extends OpenSilexModule implements APIExtension {

    @Override
    public Class<?> getConfigClass() {
        return ElasticConfig.class;
    }

    @Override
    public String getConfigId() {
        return "elastic";
    }

    private ElasticService elastic;

    @Override
    public void setup() throws Exception {
        ElasticConfig cfg = this.getConfig(ElasticConfig.class);
        elastic = new ElasticService(cfg);
        getOpenSilex().getServiceManager().register(ElasticService.class, ElasticService.DEFAULT_ELASTIC_SERVICE, elastic);
    }

    @Override
    public void bindServices(AbstractBinder binder) {
        binder.bind(elastic).to(ElasticService.class);
    }

    public ElasticService getElasticService() {
        return elastic;
    }

}
