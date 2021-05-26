/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.elastic;

import org.opensilex.config.ConfigDescription;

/**
 *
 * @author vmigot
 */
public interface ElasticServerConfig {

    @ConfigDescription(
            value = "Elastic search host",
            defaultString = "localhost"
    )
    public String host();

    @ConfigDescription(
            value = "Elastic search port",
            defaultInt = 9200
    )
    public Integer port();

}
