/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.elastic;

import java.util.List;
import org.opensilex.config.ConfigDescription;
import org.opensilex.service.ServiceConfig;

/**
 *
 * @author vmigot
 */
public interface ElasticConfig extends ServiceConfig {

    @ConfigDescription(
            value = "Activate elastic search",
            defaultBoolean = false
    )
    public Boolean enableElasticSearch();

    @ConfigDescription(
            value = "Elastic search host/port list",
            defaultList = {"{host: localhost, port: 9200}", "{host: localhost, port: 9201}"}
    )
    public List<ElasticServerConfig> servers();

}
