//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.service;

import org.opensilex.config.*;

/**
 *
 * @author Vincent Migot
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
