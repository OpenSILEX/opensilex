package org.opensilex.fs.s3.transferManager;

import org.opensilex.fs.s3.S3FileStorageConnection;
import org.opensilex.service.ServiceDefaultDefinition;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

/**
 * Specialization of {@link S3FileStorageConnection} which use the high-level S3 Transfer Manager library
 * for file upload/download
 *
 * @apiNote Do not use this implementation for the moment, since there are issues
 * when using the {@link S3TransferManager} with an HTTPS endpoint.
 * We can't read/write the bucket since the S3 API seem not be able to connect.
 *
 * @see <a href="https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/transfer-manager.html">transfer-manager SDK</a>
 * @see <a href="https://github.com/aws/aws-sdk-java-v2/issues/3350">Issue on HTTPS usage</a>
 * @see S3TransferManager
 *
 * @author rcolin
 */
@ServiceDefaultDefinition(config = S3FsTransferManagerConfig.class)
public class S3TransferManagerStorageConnection extends S3FileStorageConnection {

    /**
     * High level S3 file upload/download manager
     */
    private final S3TransferManager transferManager;

    public S3TransferManagerStorageConnection(S3FsTransferManagerConfig config){
        super(config);

        transferManager = S3TransferManagerStorageConnection.getTransferManager(config, region);
        LOGGER.info("S3TransferManager init [OK]");
    }

    @Override
    public S3FsTransferManagerConfig getConfig() {
        return (S3FsTransferManagerConfig) super.getConfig();
    }

    private static S3TransferManager getTransferManager(S3FsTransferManagerConfig config, Region region) {

        return S3TransferManager.builder()
                .s3ClientConfiguration(builder -> builder
                        .region(region)
                        .credentialsProvider(S3FileStorageConnection.getCredentialsProvider(config))
                        .endpointOverride(URI.create(config.endpoint()))
                        .targetThroughputInGbps(config.targetThroughputInGbps())
                        .minimumPartSizeInBytes(config.minimumPartSizeInBytes())
                ).build();
    }
    @Override
    public byte[] readFileAsByteArray(Path filePath) throws IOException {

        String fileKey = filePath.toString();

        // performs non-blocking download with key and bucket
        Download<ResponseBytes<GetObjectResponse>> download = transferManager.download(builder -> builder
                .getObjectRequest(requestBuilder -> setKeyAndBucket(requestBuilder, fileKey))
                .responseTransformer(AsyncResponseTransformer.toBytes()));

        // block until download completion
        CompletedDownload<ResponseBytes<GetObjectResponse>> completedDownload = download.completionFuture().join();

        // return asByteArrayUnsafe() instead of asByteArray() since this last method performs an Array copy of content
        return completedDownload.result().asByteArrayUnsafe();
    }

    @Override
    public void writeFile(Path filePath, byte[] content) throws IOException {
        String fileKey = filePath.toString();

        // performs non-blocking file content upload with key and bucket
        Upload upload = transferManager.upload(builder -> builder
                .requestBody(AsyncRequestBody.fromBytes(content))
                .putObjectRequest(requestBuilder -> setKeyAndBucket(requestBuilder, fileKey)));

        // wait for content upload completion
        upload.completionFuture().join();
    }

    @Override
    public void writeFile(Path filePath, File file) throws IOException {

        String fileKey = filePath.toString();

        // performs non-blocking file upload with key and bucket
        FileUpload upload = transferManager.uploadFile(builder -> builder
                .source(file.toPath())
                .putObjectRequest(requestBuilder -> setKeyAndBucket(requestBuilder, fileKey)));

        // wait for file upload completion
        upload.completionFuture().join();
    }


    @Override
    public void shutdown() throws Exception {
        super.shutdown();
        if (transferManager != null) {
            transferManager.close();
        }
    }

}
