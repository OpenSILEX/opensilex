//******************************************************************************
//                            LoginExtension.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.rest.extensions;

import com.auth0.jwt.JWTCreator;
import org.opensilex.rest.authentication.AuthenticationService;
import org.opensilex.rest.user.dal.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension interface for OpenSilex modules which want to add custom claims to
 * JWT token or acces them.
 *
 * @author Vincent Migot
 */
public interface LoginExtension {

    public static final Logger LOGGER = LoggerFactory.getLogger(LoginExtension.class);

    /**
     * Extension method to allow modules to add custom claims on User login in
     * JWT token. This token is available both on server and client sides.
     *
     * @param user Current user
     * @param tokenBuilder Token builder on which to add claims
     */
    public default void login(UserModel user, JWTCreator.Builder tokenBuilder) {
        LOGGER.debug(this.getClass().getCanonicalName() + " - User logged in: " + user.getEmail());
    }

    /**
     * Allow module tom implements custom logic on user logout. Do nothing by
     * default
     *
     * @param user User logged out
     */
    public default void logout(UserModel user) {
        LOGGER.debug(this.getClass().getCanonicalName() + " - User logged out: " + user.getEmail());
    }
}
