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
import software.amazon.awssdk.core.sync.RequestBody;
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
     * Thread-safe S3 client
     */
    private final S3Client s3Client;

    /**
     * Specific region used for the endpoint
     */
    private final Region region;

    /**
     * High level S3 file upload/download manager
     */
    private final S3TransferManager transferManager;

    /**
     *
     * @param config
     * @throws IllegalArgumentException if the {@link S3FsConfig#region()} is invalid
     */
    public S3FileStorageConnection(S3FsConfig config) throws URISyntaxException {
        super(config);

        region = Region.of(config.region());

        if(StringUtils.isEmpty(config.bucket())){
            throw new IllegalArgumentException("Null or empty bucket()) for S3 config");
        }

        s3Client = initClientAndRegion(config);
        createBucketIfNotExists();
        transferManager = getTransferManager(config);
    }

    @Override
    public S3FsConfig getConfig(){
        return (S3FsConfig) super.getConfig();
    }

    /**
     *
     * @param config S3 config
     *
     * @throws URISyntaxException if the {@link S3FsConfig#endpoint()} can't be parsed as an {@link URI}
      */
    private S3Client initClientAndRegion(S3FsConfig config) throws URISyntaxException {

        // Explicitly set the Credentials provider, here we use a provider which look for environment variables in order to retrieve access_key_id and secret_access_key
        // Setting the provider reduce startup time and is more explicit
        // https://rules.sonarsource.com/java/RSPEC-6242/ or https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html
        AwsCredentialsProvider credentialsProvider = EnvironmentVariableCredentialsProvider.create();

       // build the client
       return S3Client.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .endpointOverride(new URI(config.endpoint()))
                .build();
    }

    private void createBucketIfNotExists(){

        try{
            // performs request to check if the bucket exists
            s3Client.headBucket(HeadBucketRequest.builder()
                    .bucket(getConfig().bucket())
                    .build());

        }catch (NoSuchBucketException e){
            // bucket no yet created, try to create it
            s3Client.createBucket(CreateBucketRequest.builder()
                    .createBucketConfiguration(builder -> builder.locationConstraint(region.id()))
                    .bucket(getConfig().bucket())
                    .build());
        }
    }

    private S3TransferManager getTransferManager(S3FsConfig config){
        return S3TransferManager.builder()
                .s3ClientConfiguration(builder ->
                        builder.endpointOverride(URI.create(config.endpoint()))
                                .region(region)
                                .credentialsProvider(EnvironmentVariableCredentialsProvider.create()))
                .build();
    }



    @Override
    public byte[] readFileAsByteArray(Path filePath) throws IOException {
        String fileKey = filePath.toString();

        // performs non-blocking download with key and bucket
        Download<ResponseBytes<GetObjectResponse>> download =  transferManager.download(builder -> builder
                .getObjectRequest(requestBuilder -> setKeyAndBucket(requestBuilder, fileKey))
                .responseTransformer(AsyncResponseTransformer.toBytes()));

        // block until download completion
        CompletedDownload<ResponseBytes<GetObjectResponse>> completedDownload = download.completionFuture().join();

        // return asByteArrayUnsafe() instead of asByteArray() since this last method performs an Array copy of content
        return completedDownload.result().asByteArrayUnsafe();
    }

    private void setKeyAndBucket(GetObjectRequest.Builder requestBuilder, String fileKey){
         requestBuilder.key(fileKey).bucket(getConfig().bucket());
    }


    @Override
    public void writeFile(Path filePath, String content) throws IOException {
        String fileKey = filePath.toString();

        // performs non-blocking upload with key and bucket
        Upload upload = transferManager.upload(builder -> builder
                .requestBody(AsyncRequestBody.fromString(content))
                .putObjectRequest(requestBuilder -> setKeyAndBucket(requestBuilder, fileKey)));

        // wait for content upload completion
        upload.completionFuture().join();
    }

    @Override
    public void writeFile(Path filePath, File file) throws IOException {

        String fileKey = filePath.toString();

        // performs non-upload download with key and bucket
        FileUpload upload = transferManager.uploadFile(builder -> builder
                .source(file.toPath())
                .putObjectRequest(requestBuilder -> setKeyAndBucket(requestBuilder, fileKey)));

        // wait for file upload completion
        upload.completionFuture().join();
    }

    private void setKeyAndBucket(PutObjectRequest.Builder requestBuilder, String fileKey){
        requestBuilder.bucket(getConfig().bucket())
                .key(fileKey);
    }

    @Override
    public void createDirectories(Path directoryPath) {

        String directoryKey = directoryPath.toString();

        s3Client.putObject(
                builder -> setKeyAndBucket(builder, directoryKey),
                RequestBody.empty()
        );

    }

    @Override
    public boolean exist(Path filePath) throws IOException {
        String fileKey = filePath.toString();

        // performs a HEAD object (no retrieval of object himself)
        try{
            s3Client.headObject(builder -> builder
                    .bucket(getConfig().bucket())
                    .key(fileKey));
            return true;
        }catch (NoSuchKeyException e){
            return false;
        }
    }

    @Override
    public void delete(Path filePath) throws IOException {

        String fileKey = filePath.toString();

        s3Client.deleteObject(builder -> builder
                .bucket(getConfig().bucket())
                .key(fileKey));
    }

    @Override
    public Path getAbsolutePath(Path filePath) throws IOException {
        return filePath;
    }
}
