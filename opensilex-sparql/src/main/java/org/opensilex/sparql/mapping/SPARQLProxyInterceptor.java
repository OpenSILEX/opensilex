//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: opensilex@groupes.renater.fr
//******************************************************************************
package org.opensilex.sparql.mapping;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Method interceptor shared by every instance of a generated {@link SPARQLProxy} class.
 * <p>
 * Each proxy stores its handler in the {@link #HANDLER_FIELD} instance field, so one proxy class can be generated per
 * proxied type instead of one per proxy. Methods called by the proxied type constructor run before this field is set:
 * they are dispatched to the handler of the proxy under construction, as when the handler was a static field.
 * <p>
 * Must stay public: it is called from generated classes, which live in other packages and class loaders.
 */
public final class SPARQLProxyInterceptor {

    /**
     * Name of the instance field holding the proxy handler in generated classes.
     */
    static final String HANDLER_FIELD = "opensilex$proxyHandler";

    /**
     * Handler of the proxy being constructed on the current thread, read until {@link #HANDLER_FIELD} is set.
     */
    private static final ThreadLocal<InvocationHandler> HANDLER_IN_CONSTRUCTION = new ThreadLocal<>();

    private SPARQLProxyInterceptor() {
    }

    /**
     * Create a proxy instance, dispatching the methods called by its constructor to the given handler.
     *
     * @param constructor no-arg constructor of the generated proxy class
     * @param handler     handler of the new proxy
     * @return the new proxy, whose handler field is not set yet
     */
    static Object newInstance(Constructor<?> constructor, InvocationHandler handler) throws ReflectiveOperationException {
        // Save the outer handler: loading data from a constructor call may create other proxies on this thread
        InvocationHandler outerHandler = HANDLER_IN_CONSTRUCTION.get();
        HANDLER_IN_CONSTRUCTION.set(handler);
        try {
            return constructor.newInstance();
        } finally {
            if (outerHandler == null) {
                HANDLER_IN_CONSTRUCTION.remove();
            } else {
                HANDLER_IN_CONSTRUCTION.set(outerHandler);
            }
        }
    }

    @RuntimeType
    public static Object intercept(@This Object proxy,
                                   @FieldValue(HANDLER_FIELD) InvocationHandler handler,
                                   @Origin Method method,
                                   @AllArguments Object[] args) throws Throwable {
        InvocationHandler target = (handler != null) ? handler : HANDLER_IN_CONSTRUCTION.get();
        return target.invoke(proxy, method, args);
    }
}
