//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.fs.local;

import org.opensilex.config.ConfigDescription;

public interface LocalFileSystemConfig {
    
    @ConfigDescription(
        value = "Path to OpenSilex storage directory",
        defaultString = "/path/to/storage/"
    )
    String directory();

}
