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
import org.apache.jena.graph.*;
import org.opensilex.OpenSilex;
import org.opensilex.sparql.service.SPARQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vincent
 */
abstract class SPARQLProxy<T> implements InvocationHandler {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(SPARQLProxy.class);
    
    public SPARQLProxy(Node graph, Class<T> type, String lang, SPARQLService service) {
        this.type = type;
        this.service = service;
        this.graph = graph;
        this.lang = lang;
    }
    
    protected final Class<T> type;
    protected final SPARQLService service;
    protected final Node graph;
    protected final String lang;
    protected T instance;
    
    public T getInstance() {
        Class<? extends T> proxy = new ByteBuddy()
                .subclass(type)
                .implement(SPARQLProxyMarker.class)
                .method(ElementMatchers.any())
                .intercept(InvocationHandlerAdapter.of(this))
                .make()
                .load(OpenSilex.getClassLoader())
                .getLoaded();
        
        try {
            return proxy.getConstructor().newInstance();
        } catch (Exception ex) {
            LOGGER.error("Error while creating SPARQL proxy class (should never happend)", ex);
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
