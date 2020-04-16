//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.service;

import org.opensilex.config.ConfigDescription;

/**
 * Service definition interface.
 *
 * @author Vincent Migot
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
