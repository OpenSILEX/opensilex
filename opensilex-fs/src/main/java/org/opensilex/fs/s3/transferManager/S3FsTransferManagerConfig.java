package org.opensilex.fs.s3.transferManager;

import org.opensilex.config.ConfigDescription;
import org.opensilex.fs.s3.S3FsConfig;
import software.amazon.awssdk.transfer.s3.S3ClientConfiguration;

/**
 * Specialization of {@link S3FsConfig} in order to initialize a {@link org.opensilex.fs.s3.transferManager.S3TransferManagerStorageConnection}
 * @author rcolin
 */
public interface S3FsTransferManagerConfig extends S3FsConfig {

    /**
     * @return the optional minimum part size for transfer parts
     *
     * @see S3ClientConfiguration.Builder#minimumPartSizeInBytes
     */
    @ConfigDescription(
            value = "8000000L",
            defaultLong = 8000000L
    )
    long minimumPartSizeInBytes();

    /**
     * @return the optional target throughput
     *
     * @see S3ClientConfiguration.Builder#targetThroughputInGbps
     */
    @ConfigDescription(
            value = "5.0",
            defaultDouble = 5.0
    )
    double targetThroughputInGbps();

}

