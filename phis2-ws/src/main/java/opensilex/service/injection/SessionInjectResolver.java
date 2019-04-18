//******************************************************************************
//                         SessionInjectResolver.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: August 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.injection;

import javax.inject.Inject;
import javax.inject.Named;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import opensilex.service.authentication.Session;

/**
 * Session inject resolver.
 * @see https://jersey.java.net/documentation/latest/ioc.html
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
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
