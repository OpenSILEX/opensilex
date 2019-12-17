//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core;

import com.auth0.jwt.JWTCreator;
import java.util.List;
import org.opensilex.OpenSilexModule;
import org.opensilex.rest.extensions.APIExtension;
import org.opensilex.rest.security.dal.UserModel;
import org.opensilex.rest.extensions.LoginExtension;

/**
 * Core OpenSILEX module implementation
 */
public class CoreModule extends OpenSilexModule implements APIExtension, LoginExtension {

    @Override
    public List<String> getPackagesToScan() {
        List<String> list = APIExtension.super.getPackagesToScan();

        return list;
    }

    @Override
    public void addLoginClaims(UserModel user, JWTCreator.Builder tokenBuilder) {
        // TODO add experiments, projects, infrastructures related to the user ...
    }
}
