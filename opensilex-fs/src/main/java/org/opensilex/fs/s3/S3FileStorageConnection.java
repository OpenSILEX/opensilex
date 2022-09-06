package org.opensilex.fs.s3;

import org.apache.commons.lang3.StringUtils;
import org.opensilex.fs.service.FileStorageConnection;
import org.opensilex.service.BaseService;
import org.opensilex.service.ServiceDefaultDefinition;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.transfer.s3.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

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
    private final Region region;

    /**
     *
     */
    private final String bucket;

    /**
     *
     */
    private S3TransferManager transferManager;

    /**
     *
     * @param config
     * @throws IllegalArgumentException if the {@link S3FsConfig#region()} is invalid
     */
    public S3FileStorageConnection(S3FsConfig config) {
        super(config);

        region = Region.of(config.region());

        if(StringUtils.isEmpty(config.bucketName())){
            throw new IllegalArgumentException("Null or empty bucketName() for S3 config");
        }
        bucket = config.bucketName();
    }

    @Override
    public void startup() throws Exception {
        S3FsConfig config = (S3FsConfig) getConfig();

        initClientAndRegion(config);
        createBucketIfNotExists();
        initTransferManager(config);
    }

    /**
     *
     * @param config S3 config
     *
     * @throws URISyntaxException if the {@link S3FsConfig#endpoint()} can't be parsed as an {@link URI}
      */
    private void initClientAndRegion(S3FsConfig config) throws URISyntaxException {

        // Explicitly set the Credentials provider, here we use a provider which look for environment variables in order to retrieve access_key_id and secret_access_key
        // Setting the provider reduce startup time and is more explicit
        // https://rules.sonarsource.com/java/RSPEC-6242/ or https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html
        AwsCredentialsProvider credentialsProvider = EnvironmentVariableCredentialsProvider.create();

       // build the client
        s3Client = S3Client.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .endpointOverride(new URI(config.endpoint()))
                .build();
    }

    private void createBucketIfNotExists(){

        try{
            // performs request to check if the bucket exists
            s3Client.headBucket(HeadBucketRequest.builder()
                    .bucket(bucket)
                    .build());

        }catch (NoSuchBucketException e){
            // bucket no yet created, try to create it
            s3Client.createBucket(CreateBucketRequest.builder()
                    .createBucketConfiguration(builder -> builder.locationConstraint(region.id()))
                    .bucket(bucket)
                    .build());
        }
    }

    private void initTransferManager(S3FsConfig config){
        transferManager = S3TransferManager.builder()
                .s3ClientConfiguration(builder ->
                        builder.endpointOverride(URI.create(config.endpoint()))
                                .region(region)
                                .credentialsProvider(EnvironmentVariableCredentialsProvider.create()))
                .build();
    }



    @Override
    public byte[] readFileAsByteArray(Path filePath) throws IOException {
        String accessKey = filePath.toString();

        Download<ResponseBytes<GetObjectResponse>> download =  transferManager.download(builder -> builder
                .getObjectRequest(buildGetObjectRequest(accessKey))
                .responseTransformer(AsyncResponseTransformer.toBytes()));

        CompletedDownload<ResponseBytes<GetObjectResponse>> completedDownload = download.completionFuture().join();

        // return asByteArrayUnsafe() instead of asByteArray() since this last method performs an Array copy of content
        return completedDownload.result().asByteArrayUnsafe();
    }

    /**
     *
     * @param accessKey
     * @return
     */
    private GetObjectRequest buildGetObjectRequest(String accessKey){
        return GetObjectRequest.builder()
                .key(accessKey)
                .bucket(bucket)
                .build();
    }


    /**
     *
     * @param accessKey
     * @return
     */
    private PutObjectRequest buildPutObjectRequest(String accessKey){
        return PutObjectRequest.builder()
                .bucket(bucket)
                .key(accessKey)
                .build();
    }

    @Override
    public void writeFile(Path filePath, String content) throws IOException {
        String accessKey = filePath.toString();

        Upload upload = transferManager.upload(builder -> builder
                .requestBody(AsyncRequestBody.fromString(content))
                .putObjectRequest(buildPutObjectRequest(accessKey)));

        // wait for content upload completion
        upload.completionFuture().join();
    }

    @Override
    public void writeFile(Path filePath, File file) throws IOException {

        String accessKey = filePath.toString();

        FileUpload upload = transferManager.uploadFile(builder -> builder
                .source(file.toPath())
                .putObjectRequest(buildPutObjectRequest(accessKey)));

        // wait for file upload completion
        upload.completionFuture().join();
    }

    @Override
    public void createDirectories(Path directoryPath) {

        DirectoryUpload upload = transferManager.uploadDirectory(builder -> builder
                .sourceDirectory(directoryPath));

        // wait for directory upload completion
        upload.completionFuture().join();
    }

    @Override
    public boolean exist(Path filePath) throws IOException {
        return false;
    }

    @Override
    public void delete(Path filePath) throws IOException {
        s3Client.deleteObject(builder -> builder.bucket("").key(""));
    }

    @Override
    public Path getAbsolutePath(Path filePath) throws IOException {
        return filePath;
    }
}
