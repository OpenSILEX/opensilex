/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.service;

import org.opensilex.config.ConfigDescription;

/**
 *
 * @author vince
 */
public interface ServiceDefinition {

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
            value = "Service connection id"
    )
    public String serviceID();

    @ConfigDescription(
            value = "Service connection class",
            defaultClass = Service.class
    )
    public Class<? extends Service> serviceClass();
}