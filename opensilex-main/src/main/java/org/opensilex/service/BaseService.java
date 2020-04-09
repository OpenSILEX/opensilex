/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.service;

import org.opensilex.OpenSilex;

/**
 *
 * @author vince
 */
public class BaseService implements Service {

    private ServiceConstructorArguments constructorArgs;

    @Override
    public void setServiceConstructorArguments(ServiceConstructorArguments args) {
        this.constructorArgs = args;
    }

    @Override
    public ServiceConstructorArguments getServiceConstructorArguments() {
        return this.constructorArgs;
    }

    private OpenSilex opensilex;

    @Override
    public void setOpenSilex(OpenSilex opensilex) {
        this.opensilex = opensilex;
    }

    @Override
    public OpenSilex getOpenSilex() {
        return this.opensilex;
    }

}
