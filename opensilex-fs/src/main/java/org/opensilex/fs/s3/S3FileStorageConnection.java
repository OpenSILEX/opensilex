package org.opensilex.fs.s3;

import org.apache.commons.lang3.StringUtils;
import org.opensilex.fs.service.FileStorageConnection;
import org.opensilex.service.BaseService;
import org.opensilex.service.ServiceDefaultDefinition;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;


@ServiceDefaultDefinition(config = S3FsConfig.class)
public class S3FileStorageConnection extends BaseService implements FileStorageConnection {

    private static S3Client S3_CLIENT_INSTANCE;

    public S3FileStorageConnection(S3FsConfig config) {
        super(config);
    }

    @Override
    public void startup() throws Exception {
        S3FsConfig config = (S3FsConfig) getConfig();

        initClient(config);
        initBucket(config);
    }


    /**
     *
     * @param config S3 config
     *
     * @throws IllegalArgumentException if the {@link S3FsConfig#region()} is invalid
     * @throws URISyntaxException if the {@link S3FsConfig#endpoint()} can't be parsed as an {@link URI}
      */
    private static void initClient(S3FsConfig config) throws URISyntaxException {

        if(S3_CLIENT_INSTANCE != null){
            return;
        }

        S3ClientBuilder builder = S3Client.builder();
        if(!StringUtils.isEmpty(config.region())){
            builder.region(Region.of(config.region()));
        }
        // use env variable credentials
        S3_CLIENT_INSTANCE = builder
                .endpointOverride(new URI(config.endpoint()))
                .build();

    }

    private void initBucket(S3FsConfig config){

        try{
            // performs request to check if the bucket exists
            S3_CLIENT_INSTANCE.headBucket(HeadBucketRequest.builder()
                    .bucket(config.bucketName())
                    .build());

        }catch (NoSuchBucketException e){

            // create the bucket
            S3_CLIENT_INSTANCE.createBucket(CreateBucketRequest.builder()
                    .bucket(config.bucketName())
                    .build());
        }

    }




    @Override
    public byte[] readFileAsByteArray(Path filePath) throws IOException {
        return new byte[0];
    }

    @Override
    public void writeFile(Path filePath, String content) throws IOException {

    }

    @Override
    public void writeFile(Path filePath, File file) throws IOException {

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
