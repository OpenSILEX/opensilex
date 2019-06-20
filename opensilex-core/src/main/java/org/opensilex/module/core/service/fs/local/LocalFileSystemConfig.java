package org.opensilex.module.core.service.fs.local;

import org.opensilex.config.ConfigDescription;

public interface LocalFileSystemConfig {
    
    @ConfigDescription(
        value = "Path to OpenSilex storage directory",
        defaultString = "/path/to/storage/"
    )
    String directory();

}
