//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.module.extensions;

import java.util.ArrayList;
import java.util.List;
import org.opensilex.server.rest.RestApplication;

/**
 *
 * @author Vincent Migot
 */
public interface APIExtension {

    /**
     * This method is called during application initialization to get all
     * packages to scan for components like request filters or response mapper
     *
     * @return List of packages to scan
     */
    public default List<String> getPackagesToScan() {
        List<String> list = new ArrayList<>();
        list.addAll(apiPackages());

        return list;
    }

    /**
     * This method is called during application initialization to get all
     * packages to scan for jersey web services wich will be displayed into
     * swagger UI
     *
     * @return List of packages to scan for web services
     */
    public default List<String> apiPackages() {
        List<String> list = new ArrayList<>();
        list.add(getClass().getPackage().getName());

        return list;
    }

    /**
     * This entry point allow module to initialize anything in application after
     * all configuration is loaded at the end of application loading
     * 
     * @param resourceConfig API main entry point instance extending Jersey {@code org.glassfish.jersey.server.ResourceConfig}
     */
    public default void initAPI(RestApplication resourceConfig) {
        // Do nothing by default; 
    }
}
