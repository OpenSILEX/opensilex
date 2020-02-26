//******************************************************************************
//                  LocalFileSystemConfig.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.fs.local;

import org.opensilex.config.ConfigDescription;

/**
 * Local file system configuration interface
 *
 * @see org.opensilex.fs.local.LocalFileSystemConnection
 * @author Vincent Migot
 */
public interface LocalFileSystemConfig {


    @ConfigDescription(
        value = "Base path for file storage"
    )
    public String storageBasePath();

}
