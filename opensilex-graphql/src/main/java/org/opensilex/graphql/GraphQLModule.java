/*******************************************************************************
 *                         GraphQLModule.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2023.
 * Last Modification: 05/10/2023
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.graphql;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.opensilex.OpenSilexModule;
import org.opensilex.graphql.staple.StapleApiUtils;
import org.opensilex.server.extensions.APIExtension;

/**
 * APIs needed to make GraphQL work with OpenSILEX.
 *
 * @author Valentin Rigolle
 */
public class GraphQLModule extends OpenSilexModule implements APIExtension {
    @Override
    public void bindServices(AbstractBinder binder) {
        binder.bindAsContract(StapleApiUtils.class).in(RequestScoped.class);
    }
}
