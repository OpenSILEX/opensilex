/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.service;

import org.glassfish.hk2.api.Factory;

/**
 *
 * @author vmigot
 */
public abstract class ServiceFactory<T extends Service> implements Factory<T>, Service {

    public abstract Class<T> getServiceClass();

}
