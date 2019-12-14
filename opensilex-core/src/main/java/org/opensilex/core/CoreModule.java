//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core;

import com.auth0.jwt.JWTCreator;
import org.opensilex.module.OpenSilexModule;
import org.opensilex.module.extensions.APIExtension;
import org.opensilex.module.extensions.TokenExtension;
import org.opensilex.server.user.dal.UserModel;

/**
 * Core OpenSILEX module implementation
 */
public class CoreModule extends OpenSilexModule implements APIExtension, TokenExtension {

    @Override
    public void addLoginClaims(UserModel user, JWTCreator.Builder tokenBuilder) {
        // TODO add experiments, projects, infrastructures related to the user ...
    }
}
