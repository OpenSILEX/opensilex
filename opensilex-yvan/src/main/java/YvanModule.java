/*
 * *****************************************************************************
 *                         YvanWsModule.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 26/06/2026 10:14
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

import org.opensilex.OpenSilexModule;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.extensions.APIExtension;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;

/**
 * opensilex-yvan opensilex module implementation
 */
public class YvanModule extends OpenSilexModule implements APIExtension {
    @Inject
    private SPARQLService sparql;

    @CurrentUser
    AccountModel currentUser;



}
