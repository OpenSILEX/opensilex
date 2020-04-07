/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.security.authentication.injection;

import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.opensilex.security.user.dal.UserModel;

/**
 *
 * @author vince
 */
public class CurrentUserFactory implements Factory<UserModel> {

    private final ServiceLocator locator;

    @Inject
    public CurrentUserFactory(ServiceLocator locator) {
        this.locator = locator;
    }

    @Override
    public UserModel provide() {
        SecurityContext ctx = locator.getService(SecurityContext.class);
        return (UserModel) ctx.getUserPrincipal();
    }

    @Override
    public void dispose(UserModel t) {
        //
    }

}
