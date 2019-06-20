/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.service.sparql;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 *
 * @author vincent
 */
abstract class SPARQLProxy<T> implements InvocationHandler {

    public SPARQLProxy(Class<T> type, SPARQLService service) {
        this.type = type;
        this.service = service;
    }

    protected Class<T> type;
    protected SPARQLService service;
    protected T instance;

    @SuppressWarnings("unchecked")
    public T getInstance() {
        return (T) java.lang.reflect.Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{type},
                this
        );
    }

    private boolean loaded = false;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (!loaded) {
            instance = loadData();
            loaded = true;
        }

        return method.invoke(instance, args);
    }

    protected abstract T loadData() throws Exception;

}
