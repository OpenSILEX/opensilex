//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.service;

import org.glassfish.hk2.api.Factory;

/**
 * Abstract service factory definition.
 *
 * @author Vincent Migot
 * @param <T> Provided service class
 */
public abstract class ServiceFactory<T extends Service> extends BaseService implements Factory<T>, Service {

    public ServiceFactory(ServiceConfig config) {
        super(config);
    }

    /**
     * Return factory provided service class.
     *
     * @return provided service class
     */
    public abstract Class<T> getServiceClass();

}
