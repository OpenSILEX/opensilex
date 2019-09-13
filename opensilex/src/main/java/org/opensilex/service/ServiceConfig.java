/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.service;

import org.opensilex.config.ConfigDescription;

/**
 *
 * @author vincent
 */
public interface ServiceConfig {

    @ConfigDescription(
            value = "Service implementation class",
            defaultClass = Service.class
    )
    public Class<? extends Service> implementation();

    @ConfigDescription(
            value = "Service configuration class"
    )
    public Class<?> configClass();

    @ConfigDescription(
            value = "Service configuration id"
    )
    public String configID();

    @ConfigDescription(
            value = "Service connection configuration class"
    )
    public Class<?> connectionConfig();

    @ConfigDescription(
            value = "Service connection configuration id"
    )
    public String connectionConfigID();

    @ConfigDescription(
            value = "Service connection class",
            defaultClass = ServiceConnection.class
    )
    public Class<? extends ServiceConnection> connection();

}
