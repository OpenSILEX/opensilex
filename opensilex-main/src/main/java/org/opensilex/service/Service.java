//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.service;

import org.opensilex.OpenSilex;

/**
 * Interface for service definition.
 *
 * @author Vincent Migot
 */
public interface Service {

    /**
     * Method to initialialize all service setup configuration (before service "startup" but after all modules "setup").
     *
     * @throws Exception in case of setup error
     */
    public default void setup() throws Exception {

    }

    /**
     * Method to start service (when all modules and services are "setup").
     *
     * @throws Exception in case of startup error
     */
    public default void startup() throws Exception {

    }

    /**
     * Method to stop service (after all modules "shutdown" but before all modules and services "clean").
     *
     * @throws Exception in case of shutdown error
     */
    public default void shutdown() throws Exception {

    }

    /**
     * Method to clean service state (after all services "shutdown").
     *
     * @throws Exception
     */
    public default void clean() throws Exception {

    }

    /**
     * Define OpenSilex instance (see org.opensilex.service.BaseService for default implementation).
     *
     * @param opensilex current application instance
     */
    public void setOpenSilex(OpenSilex opensilex);

    /**
     * Return current OpenSilex instance (see org.opensilex.service.BaseService for default implementation).
     *
     * @return current application instance
     */
    public OpenSilex getOpenSilex();

    public ServiceConfig getConfig();
}
