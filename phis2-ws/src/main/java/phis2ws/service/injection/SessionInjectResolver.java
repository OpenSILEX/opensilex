//**********************************************************************************************
//                                       SessionInjectResolver.java 
//
// Author(s): Arnaud CHARLEROY
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: august 2016
// Contact:arnaud.charleroy@supagro.inra.fr, anne.tireau@supagro.inra.fr, pascal.neveu@supagro.inra.fr
// Last modification date:  October, 2016
// Subject: Recognize sessionInject utilisation in jersey resources
// The SessionInjectResolver above just delegates to the default HK2 system injection resolver to do the actual work.
// see https://jersey.java.net/documentation/latest/ioc.html
//***********************************************************************************************
package phis2ws.service.injection;

import javax.inject.Inject;
import javax.inject.Named;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import phis2ws.service.authentication.Session;

/**
 * Permet de repérer l'utilisation de l'annotation Session Inject
 * @see https://jersey.java.net/documentation/latest/ioc.html
 * @author A. CHARLEROY
 */
public class SessionInjectResolver implements InjectionResolver<SessionInject> {

    @Inject
    @Named(InjectionResolver.SYSTEM_RESOLVER_NAME)
    InjectionResolver<Inject> SYSTEM_INJECTION_RESOLVER;

    @Override
    public Object resolve(Injectee injectee, ServiceHandle<?> handle) {
        if (Session.class == injectee.getRequiredType()) {
            return SYSTEM_INJECTION_RESOLVER.resolve(injectee, handle);
        }

        return null;
    }

    @Override
    public boolean isConstructorParameterIndicator() {
        return false;
    }

    @Override
    public boolean isMethodParameterIndicator() {
        return false;
    }
}
