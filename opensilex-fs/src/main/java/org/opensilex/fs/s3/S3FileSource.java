package org.opensilex.fs.s3;


import org.opensilex.fs.source.FileSource;

import java.util.Map;
import java.util.Objects;

/**
 * Unique connection to a S3 storage with (endpoint,region,bucket)
 */
public class S3FileSource extends FileSource {

    public S3FileSource() {
    }

    public S3FileSource(S3FsConfig config) {
        super(
                null,
                "s3",
                Map.of(
                        "bucket", config.bucket(),
                        "region", config.region(),
                        "endpoint", config.endpoint()
                )
        );
    }

}
