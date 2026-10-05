/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.mapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.MethodDelegation;
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

    /**
     * Proxy class generated once per proxied type. Generating it for each proxy (with its own class loader) cost
     * more than the data it loads: allocations, CPU and metaspace, until the class was unloaded.
     */
    private static final ClassValue<ProxyClass> PROXY_CLASSES = new ClassValue<ProxyClass>() {
        @Override
        protected ProxyClass computeValue(Class<?> type) {
            Class<?> proxyClass = new ByteBuddy()
                    .subclass(type)
                    .implement(SPARQLProxyMarker.class)
                    .defineField(SPARQLProxyInterceptor.HANDLER_FIELD, InvocationHandler.class, Visibility.PUBLIC)
                    .method(ElementMatchers.any())
                    .intercept(MethodDelegation.to(SPARQLProxyInterceptor.class))
                    .make()
                    .load(OpenSilex.getClassLoader())
                    .getLoaded();

            try {
                return new ProxyClass(proxyClass.getConstructor(), proxyClass.getField(SPARQLProxyInterceptor.HANDLER_FIELD));
            } catch (NoSuchMethodException | NoSuchFieldException ex) {
                throw new IllegalStateException("Invalid SPARQL proxy class generated for " + type.getName(), ex);
            }
        }
    };

    private static final class ProxyClass {
        private final Constructor<?> constructor;
        private final Field handlerField;

        private ProxyClass(Constructor<?> constructor, Field handlerField) {
            this.constructor = constructor;
            this.handlerField = handlerField;
        }
    }

    public SPARQLProxy(SPARQLClassObjectMapperIndex mapperIndex, Node graph, Class<T> type, String lang, SPARQLService service) {
        this.mapperIndex = mapperIndex;
        this.type = type;
        this.service = service;
        this.graph = graph;
        this.lang = lang;
    }

    protected final Class<T> type;
    protected final SPARQLService service;
    protected final Node graph;
    protected final String lang;
    protected final SPARQLClassObjectMapperIndex mapperIndex;
    protected T instance;

    public T getInstance() {
        ProxyClass proxyClass = PROXY_CLASSES.get(type);

        try {
            T proxy = type.cast(SPARQLProxyInterceptor.newInstance(proxyClass.constructor, this));
            proxyClass.handlerField.set(proxy, this);
            return proxy;
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
    
    protected boolean isLoaded() {
        return this.loaded;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        loadIfNeeded();
        return method.invoke(instance, args);
    }

    protected abstract T loadData() throws Exception;

}
