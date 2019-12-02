//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.security;

import java.security.Principal;
import javax.ws.rs.core.SecurityContext;
import org.opensilex.server.user.dal.UserModel;

/**
 *
 * @author vidalmor
 */
public class SecurityContextProxy implements SecurityContext {

    SecurityContext parentContext;
    UserModel user;

    public SecurityContextProxy(SecurityContext parentContext, UserModel user) {
        this.parentContext = parentContext;
        this.user = user;
    }

    @Override
    public Principal getUserPrincipal() {
        return user;
    }

    @Override
    public boolean isUserInRole(String role) {
        return parentContext.isUserInRole(role);
    }

    @Override
    public boolean isSecure() {
        return parentContext.isSecure();
    }

    @Override
    public String getAuthenticationScheme() {
        return parentContext.getAuthenticationScheme();
    }
}
