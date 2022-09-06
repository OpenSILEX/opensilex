package org.opensilex.fs.s3;

import org.apache.commons.lang3.StringUtils;
import org.opensilex.fs.service.FileStorageConnection;
import org.opensilex.service.BaseService;
import org.opensilex.service.ServiceDefaultDefinition;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.internal.waiters.ResponseOrException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Amazon S3 file-system implementation
 * @author rcolin
 */
@ServiceDefaultDefinition(config = S3FsConfig.class)
public class S3FileStorageConnection extends BaseService implements FileStorageConnection {

    /**
     * S3 client, the client is thread-safe
     */
    private S3Client s3Client;

    /**
     * Specific region used for the endpoint
     */
    private Region region;

    private final String bucket;

    public S3FileStorageConnection(S3FsConfig config) {
        super(config);
        if(StringUtils.isEmpty(config.bucketName())){
            throw new IllegalArgumentException("Null or empty bucket");
        }
        bucket = config.bucketName();
    }

    @Override
    public void startup() throws Exception {
        S3FsConfig config = (S3FsConfig) getConfig();

        initClientAndRegion(config);
        initBucket(config);
    }

    /**
     *
     * @param config S3 config
     *
     * @throws IllegalArgumentException if the {@link S3FsConfig#region()} is invalid
     * @throws URISyntaxException if the {@link S3FsConfig#endpoint()} can't be parsed as an {@link URI}
      */
    private void initClientAndRegion(S3FsConfig config) throws URISyntaxException {

        if(s3Client != null){
            return;
        }

        S3ClientBuilder builder = S3Client.builder();
        if(!StringUtils.isEmpty(config.region())){
            region = Region.of(config.region());
            builder.region(region);
        }

        // Explicitly set the Credentials provider, here we use a provider which look for environment variables in order to retrieve access_key_id and secret_access_key
        // Setting the provider reduce startup time and is more explicit
        // https://rules.sonarsource.com/java/RSPEC-6242/ https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html
        AwsCredentialsProvider credentialsProvider = EnvironmentVariableCredentialsProvider.create();

       // build the client
        s3Client = builder
                .credentialsProvider(credentialsProvider)
                .endpointOverride(new URI(config.endpoint()))
                .build();
    }

    private void initBucket(S3FsConfig config){

        try{
            // performs request to check if the bucket exists
            s3Client.headBucket(HeadBucketRequest.builder()
                    .bucket(bucket)
                    .build());

        }catch (NoSuchBucketException e){
            // bucket no yet created, try to create it
            CreateBucketRequest.Builder createBucketBuilder = CreateBucketRequest.builder();

            // set the region for bucket creation
            if(region != null){
                createBucketBuilder.createBucketConfiguration(
                        CreateBucketConfiguration.builder().
                                locationConstraint(region.id())
                                .build());
            }

            // create the bucket
            s3Client.createBucket(createBucketBuilder
                    .bucket(bucket)
                    .build());
        }

    }


    @Override
    public byte[] readFileAsByteArray(Path filePath) throws IOException {
        return new byte[0];
    }

    private void handlePutResponse(String accessKey) throws IOException {

        HeadObjectRequest requestWait = HeadObjectRequest.builder().bucket(bucket).key(accessKey).build();
        WaiterResponse<HeadObjectResponse> waiterResponse = s3Client.waiter().waitUntilObjectExists(requestWait);
        ResponseOrException<HeadObjectResponse> responseOrException = waiterResponse.matched();

        Optional<Throwable> exceptionOptional = responseOrException.exception();

        if(exceptionOptional.isPresent()){
            throw new IOException(exceptionOptional.get());
        }
    }

    private PutObjectRequest getPutObjectRequest(String accessKey, Path filePath){
        return PutObjectRequest.builder()
                .bucket(bucket)
                .key(accessKey)
                .build();
    }

    @Override
    public void writeFile(Path filePath, String content) throws IOException {
        String accessKey = filePath.toString();
        s3Client.putObject(getPutObjectRequest(accessKey,filePath), RequestBody.fromString(content));
        handlePutResponse(accessKey);
    }

    @Override
    public void writeFile(Path filePath, File file) throws IOException {
        String accessKey = filePath.toString();
        s3Client.putObject(getPutObjectRequest(accessKey,filePath), RequestBody.fromFile(file));
        handlePutResponse(accessKey);
    }

    @Override
    public void createDirectories(Path directoryPath) throws IOException {

    }

    @Override
    public boolean exist(Path filePath) throws IOException {
        return false;
    }

    @Override
    public void delete(Path filePath) throws IOException {

    }

    @Override
    public Path getAbsolutePath(Path filePath) throws IOException {
        return filePath;
    }
}
