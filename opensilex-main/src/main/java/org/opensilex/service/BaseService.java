//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.service;

import org.opensilex.OpenSilex;

/**
 * Abstract service definition.
 *
 * @author Vincent Migot
 */
public abstract class BaseService implements Service {

    /**
     * Service construction arguments definitions.
     */
    private ServiceConstructorArguments constructorArgs;

    /**
     * OpenSilex instance.
     */
    private OpenSilex opensilex;

    @Override
    public void setServiceConstructorArguments(ServiceConstructorArguments args) {
        this.constructorArgs = args;
    }

    @Override
    public ServiceConstructorArguments getServiceConstructorArguments() {
        return this.constructorArgs;
    }

    @Override
    public void setOpenSilex(OpenSilex opensilex) {
        this.opensilex = opensilex;
    }

    @Override
    public OpenSilex getOpenSilex() {
        return this.opensilex;
    }

}
