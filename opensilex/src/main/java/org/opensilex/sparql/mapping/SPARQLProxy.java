/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.mapping;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;
import org.opensilex.sparql.SPARQLService;

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

    public T getInstance() {
        Class<? extends T> proxy = new ByteBuddy()
                .subclass(type)
                .method(ElementMatchers.any())
                .intercept(InvocationHandlerAdapter.of(this))
                .make()
                .load(Thread.currentThread().getContextClassLoader())
                .getLoaded();

        try {
            return proxy.getConstructor().newInstance();
        } catch (Exception ex) {
            // TODO log error
        }

        return null;
    }

    private boolean loaded = false;

    protected T loadIfNeeded() throws Exception {
        if (!loaded) {
            instance = loadData();
            loaded = true;
        }
        
        return instance;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        loadIfNeeded();
        return method.invoke(instance, args);
    }

    protected abstract T loadData() throws Exception;

}
