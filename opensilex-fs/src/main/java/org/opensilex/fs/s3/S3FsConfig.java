package org.opensilex.fs.s3;

import org.opensilex.config.ConfigDescription;
import org.opensilex.service.ServiceConfig;

/**
 * @author colin
 */
public interface S3FsConfig extends ServiceConfig {

    @ConfigDescription(
            value = "s3-website.eu-west-3.amazonaws.com"
    )
    String endpoint();

    @ConfigDescription(
            value = "opensilex"
    )
    String bucket();

    @ConfigDescription(
            value = "eu-west-3"
    )
    String region();

}
