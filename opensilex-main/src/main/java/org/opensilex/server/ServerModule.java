/*
 * *****************************************************************************
 *                         ServerModule.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 19/04/2024 11:55
 * Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr, gabriel.besombes@inrae.fr
 * *****************************************************************************
 */
package org.opensilex.server;

import org.apache.commons.lang3.StringUtils;
import org.opensilex.OpenSilexModule;
import org.opensilex.server.extensions.APIExtension;
import org.opensilex.server.rest.cache.JCSApiCacheExtension;

import javax.ws.rs.core.UriBuilder;
import java.util.List;

/**
 * Tomcat Server integration module for OpenSilex.
 *
 * @see org.opensilex.server.ServerConfig
 * @author Vincent Migot
 */
public class ServerModule extends OpenSilexModule implements APIExtension, JCSApiCacheExtension {

    @Override
    public Class<?> getConfigClass() {
        return ServerConfig.class;
    }

    @Override
    public String getConfigId() {
        return "server";
    }

    @Override
    public List<String> getPackagesToScan() {
        List<String> list = APIExtension.super.getPackagesToScan();
        list.add("org.opensilex.server.rest.serialization");
        list.add("org.opensilex.server.rest.validation");
        list.add("org.opensilex.server.rest.cache");

        return list;
    }

    public String getBaseURL(){
        ServerConfig cfg =(ServerConfig) this.getConfig();

        UriBuilder baseUrlBuilder = UriBuilder.fromUri(((ServerConfig) this.getConfig()).publicURI());
        if(!StringUtils.isEmpty(cfg.pathPrefix())){
            baseUrlBuilder.path(cfg.pathPrefix());
        }
        return baseUrlBuilder.build().toString();
    }

    public String getAppUrl() {
        return UriBuilder.fromPath(getBaseURL()).path("/app/").build().toString();
    }
}
