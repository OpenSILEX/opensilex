package org.opensilex.fs.s3;

import org.opensilex.config.ConfigDescription;
import org.opensilex.service.ServiceConfig;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

/**
 * @author rcolin
 */
public interface S3FsConfig extends ServiceConfig {

    /**
     * @return a String representation of S3 endpoint URL
     *
     * @see <a href="https://docs.aws.amazon.com/general/latest/gr/s3.html">endpoints and quotas</a>
     */
    @ConfigDescription(
            value = "S3 endpoint URL",
            defaultString = "s3-website.eu-west-3.amazonaws.com"
    )
    String endpoint();

    /**
     *
     * @return the name of the S3 bucket in which files are stored
     */
    @ConfigDescription(
            value = "S3 bucket name",
            defaultString = "opensilex"
    )
    String bucket();

    /**
     * @return  String representation of S3 region
     *
     * @see <a href="https://docs.aws.amazon.com/general/latest/gr/s3.html">endpoints and quotas</a>
     * @see software.amazon.awssdk.regions.Region
     */
    @ConfigDescription(
            value = "S3 region code",
            defaultString = "eu-west-3"
    )
    String region();

    /**
     * @return the name of the credentials profile to use
     * 
     * @see software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider#create(String)
     * @see <a href="https://docs.aws.amazon.com/sdkref/latest/guide/file-format.html">Credentials file format</a>
     *
     */
    @ConfigDescription(
            value = "Name of the credentials profile",
            defaultString = "default"
    )
    String profileName();

    /**
     * @return true if we must use {@link software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider} (it tries each available credentials' method), false else
     *
     * @see software.amazon.awssdk.services.s3.S3ClientBuilder#credentialsProvider(AwsCredentialsProvider)
     * @see  software.amazon.awssdk.transfer.s3.S3ClientConfiguration.Builder#credentialsProvider(AwsCredentialsProvider)
     */
    @ConfigDescription(
            value = "Indicate if the default S3 credentials method must be used or not",
            defaultBoolean = false
    )
    boolean useDefaultCredentialsProvider();
}
