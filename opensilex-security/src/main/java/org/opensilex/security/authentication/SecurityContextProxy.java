//******************************************************************************
//                       SecurityContextProxy.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.authentication;

import java.security.Principal;
import javax.ws.rs.core.SecurityContext;
import org.opensilex.security.account.dal.AccountModel;

/**
 * Helper class to encapsulate an existing security context with the correct
 * User class type {@code org.opensilex.server.user.dal.AccountModel}
 *
 * @see org.opensilex.security.authentication.filters.AuthenticationFilter
 * @author Vincent Migot
 */
public class SecurityContextProxy implements SecurityContext {

    /**
     * Parent context which is proxied for all method except
     * {@code getPrincipal}
     */
    SecurityContext parentContext;

    /**
     * User to return through {@code getPrincipal} method
     */
    AccountModel user;

    /**
     * Create security context from an existing one and a user
     *
     * @param parentContext parent security context to proxy
     * @param user user to return in this context
     */
    public SecurityContextProxy(SecurityContext parentContext, AccountModel user) {
        this.parentContext = parentContext;
        this.user = user;
    }

    @Override
    public Principal getUserPrincipal() {
        // return user defined in constructor
        return user;
    }

    @Override
    public boolean isUserInRole(String role) {
        // proxy parent context
        return parentContext.isUserInRole(role);
    }

    @Override
    public boolean isSecure() {
        // proxy parent context
        return parentContext.isSecure();
    }

    @Override
    public String getAuthenticationScheme() {
        // proxy parent context
        return parentContext.getAuthenticationScheme();
    }
}
