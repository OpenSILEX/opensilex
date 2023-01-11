/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.security.authentication.injection;

import javax.inject.Inject;
import javax.inject.Named;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import org.opensilex.security.account.dal.AccountModel;

/**
 *
 * @author vince
 */
public class CurrentUserResolver implements InjectionResolver<CurrentUser> {

    private final InjectionResolver<Inject> systemResolver;

    @Inject
    public CurrentUserResolver(
            @Named(InjectionResolver.SYSTEM_RESOLVER_NAME) final InjectionResolver<Inject> systemResolver) {
        this.systemResolver = systemResolver;
    }

    @Override
    public Object resolve(Injectee injct, ServiceHandle<?> sh) {
        Class<?> clazz = (Class<?>) injct.getRequiredType();
        if (clazz.isAssignableFrom(AccountModel.class)) {
            return systemResolver.resolve(injct, sh);
        }
        return null;
    }

    @Override
    public boolean isConstructorParameterIndicator() {
        return systemResolver.isConstructorParameterIndicator();
    }

    @Override
    public boolean isMethodParameterIndicator() {
        return systemResolver.isMethodParameterIndicator();
    }

}
