package org.opensilex.fs.s3;

import org.apache.commons.lang3.StringUtils;
import org.opensilex.fs.service.FileStorageConnection;
import org.opensilex.service.BaseService;
import org.opensilex.service.ServiceDefaultDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

/**
 * Amazon S3 file-system implementation
 * @author rcolin
 */
@ServiceDefaultDefinition(config = S3FsConfig.class)
public class S3FileStorageConnection extends BaseService implements FileStorageConnection {

    protected static final Logger LOGGER =  LoggerFactory.getLogger(S3FileStorageConnection.class);

    /**
     * Thread-safe S3 client
     */
    private final S3Client s3Client;

    /**
     * Specific region used for the endpoint
     */
    protected final Region region;

    /**
     *
     * @param config S3 config
     * @throws IllegalArgumentException in the following cases :
     * <ul>
     *     <li>the {@link S3FsConfig#region()} is unknown from {@link Region} enums</li>
     *     <li>the {@link S3FsConfig#bucket()} is null or empty</li>
     * </ul>
     *
     */
    public S3FileStorageConnection(S3FsConfig config) {
        super(config);

        region = Region.of(config.region());

        if(StringUtils.isEmpty(config.bucket())){
            throw new IllegalArgumentException("Null or empty bucket()) for S3 config");
        }

        // call ClientStore in order to get a unique shared client by (endpoint,region)
        s3Client = initClientAndRegion(config);
        LOGGER.info("S3FileStorageConnection init [OK]");

        createBucketIfNotExists();
    }

    /**
     *
     * @return the credentials' provider to use for S3 connection.
     * If {@link S3FsConfig#useDefaultCredentialsProvider()} if true, then return a new {@link DefaultCredentialsProvider},
     * return a new {@link ProfileCredentialsProvider} else.
     *
     * @apiNote
     * <pre>
     * Explicitly set the Credentials provider reduce startup time and is more explicit
     * - <a href="https://rules.sonarsource.com/java/RSPEC-6242/">Sonar RSPEC-6242</a>
     * - <a href="https://aws.amazon.com/fr/blogs/developer/tuning-the-aws-java-sdk-2-x-to-reduce-startup-time/">Client startup optimization</a>
     * </pre>
     *
     * @see <a href="https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html">S3 credentials</a>
     */
    protected static AwsCredentialsProvider getCredentialsProvider(S3FsConfig config){

        if(config.useDefaultCredentialsProvider()){
            return DefaultCredentialsProvider.create();
        }

        if(StringUtils.isEmpty(config.profileName())){
            throw new IllegalArgumentException("Null or empty S3FsConfig profileName");
        }
        return ProfileCredentialsProvider.create(config.profileName());
    }

    @Override
    public S3FsConfig getConfig(){
        return (S3FsConfig) super.getConfig();
    }

    private static S3Client initClientAndRegion(S3FsConfig config) {
       return S3Client.builder()
                .region(Region.of(config.region()))
                .credentialsProvider(S3FileStorageConnection.getCredentialsProvider(config))
                .endpointOverride(URI.create(config.endpoint()))
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

            LOGGER.info("{} bucket has been created [OK]", getConfig().bucket());
        }
    }
    protected void setKeyAndBucket(GetObjectRequest.Builder requestBuilder, String fileKey){
        requestBuilder.key(fileKey).bucket(getConfig().bucket());
    }

    protected void setKeyAndBucket(PutObjectRequest.Builder requestBuilder, String fileKey){
        requestBuilder.bucket(getConfig().bucket()).key(fileKey);
    }

    protected void setKeyAndBucket(HeadObjectRequest.Builder requestBuilder, String fileKey){
        requestBuilder.bucket(getConfig().bucket()).key(fileKey);
    }

    protected void setKeyAndBucket(DeleteObjectRequest.Builder requestBuilder, String fileKey){
        requestBuilder.bucket(getConfig().bucket()).key(fileKey);
    }

    @Override
    public byte[] readFileAsByteArray(Path filePath) throws IOException {
        String fileKey = filePath.toString();
        return s3Client.getObjectAsBytes(builder -> setKeyAndBucket(builder, fileKey)).asByteArrayUnsafe();
    }

    @Override
    public void writeFile(Path filePath, byte[] content) throws IOException {
        String fileKey = filePath.toString();
        s3Client.putObject(builder -> setKeyAndBucket(builder, fileKey), RequestBody.fromBytes(content));
    }

    @Override
    public void writeFile(Path filePath, File file) throws IOException {
        String fileKey = filePath.toString();
        s3Client.putObject(builder -> setKeyAndBucket(builder, fileKey), RequestBody.fromFile(file));
    }


    @Override
    public void createDirectories(Path directoryPath) {
        String directoryKey = directoryPath.toString();
        s3Client.putObject(builder -> setKeyAndBucket(builder, directoryKey), RequestBody.empty());
    }

    @Override
    public boolean exist(Path filePath) throws IOException {
        String fileKey = filePath.toString();

        // performs a HEAD object (no retrieval of object himself -> less I/O)
        try{
            s3Client.headObject(builder -> setKeyAndBucket(builder,fileKey));
            return true;
        }catch (NoSuchKeyException e){
            return false;
        }
    }

    @Override
    public void delete(Path filePath) throws IOException {
        String fileKey = filePath.toString();
        s3Client.deleteObject(builder -> setKeyAndBucket(builder,fileKey));
    }

    @Override
    public Path getAbsolutePath(Path filePath) throws IOException {
        return filePath;
    }

    @Override
    public void shutdown() throws Exception {
        s3Client.close();
    }
}
