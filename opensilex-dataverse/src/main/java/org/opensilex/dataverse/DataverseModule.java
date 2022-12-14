//******************************************************************************
//                          DataverseModule.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: gabriel.besombes@inrae.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.dataverse;

import org.opensilex.OpenSilexModule;
import org.opensilex.server.extensions.APIExtension;

/**
 * @author Gabriel Besombes
 * dataverse opensilex module implementation
 */
public class DataverseModule extends OpenSilexModule implements APIExtension {

    @Override
    public Class<?> getConfigClass() {
        return OpensilexDataverseConfig.class;
    }

    @Override
    public String getConfigId() {
        return "dataverse";
    }
}
