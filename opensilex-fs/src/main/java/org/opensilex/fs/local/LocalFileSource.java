package org.opensilex.fs.local;

import org.opensilex.fs.source.FileSource;

import javax.validation.constraints.NotNull;
import java.util.Map;

public class LocalFileSource extends FileSource {

    public LocalFileSource() {
    }

    public LocalFileSource(@NotNull  LocalFileSystemConfig config, String hostname) {
        super(
                null,
                "local",
                Map.of(
                        "basePath", config.basePath(),
                        "hostname", hostname
                )
        );
    }

}
