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
import org.opensilex.security.account.dal.AccountModel;

/**
 *
 * @author vince
 */
public class CurrentUserFactory implements Factory<AccountModel> {

    private final ServiceLocator locator;

    @Inject
    public CurrentUserFactory(ServiceLocator locator) {
        this.locator = locator;
    }

    @Override
    public AccountModel provide() {
        SecurityContext ctx = locator.getService(SecurityContext.class);
        return (AccountModel) ctx.getUserPrincipal();
    }

    @Override
    public void dispose(AccountModel t) {
        //
    }

}
