/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.security;

import java.security.Principal;
import javax.ws.rs.core.SecurityContext;
import org.opensilex.server.security.user.User;

/**
 *
 * @author vidalmor
 */
public class SecurityContextProxy implements SecurityContext {

    SecurityContext parentContext;
    User user;

    public SecurityContextProxy(SecurityContext parentContext, User user) {
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
