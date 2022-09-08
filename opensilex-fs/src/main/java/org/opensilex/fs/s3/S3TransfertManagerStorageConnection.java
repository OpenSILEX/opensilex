package org.opensilex.fs.s3;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.transfer.s3.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

public class S3TransfertManagerStorageConnection extends S3FileStorageConnection{

    /**
     * High level S3 file upload/download manager
     */
    private final S3TransferManager transferManager;

    public S3TransfertManagerStorageConnection(S3FsConfig config) throws URISyntaxException {
        super(config);

        transferManager = getTransferManager(config);
//        LOGGER.info("S3TransferManager init [OK]");
    }

    private S3TransferManager getTransferManager(S3FsConfig config) {
        return S3TransferManager.builder()
                .s3ClientConfiguration(builder -> builder
                        .region(region)
                        .credentialsProvider(getCredentialsProvider())
                        .endpointOverride(URI.create(config.endpoint()))
//                        .targetThroughputInGbps(1.0)
//                        .minimumPartSizeInBytes(1024L*1024L)
                ).build();
    }

    @Override
    public byte[] readFileAsByteArray(Path filePath) throws IOException {

        String fileKey = filePath.toString();

//        // performs non-blocking download with key and bucket
        Download<ResponseBytes<GetObjectResponse>> download =  transferManager.download(builder -> builder
                .getObjectRequest(requestBuilder -> setKeyAndBucket(requestBuilder, fileKey))
                .responseTransformer(AsyncResponseTransformer.toBytes()));
//
//        // block until download completion
        CompletedDownload<ResponseBytes<GetObjectResponse>> completedDownload = download.completionFuture().join();
//
//        // return asByteArrayUnsafe() instead of asByteArray() since this last method performs an Array copy of content
        return completedDownload.result().asByteArrayUnsafe();
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

        FileUpload upload = transferManager.uploadFile(builder -> builder
                .source(file.toPath())
                .putObjectRequest(requestBuilder -> setKeyAndBucket(requestBuilder, fileKey)));

        // wait for file upload completion
        upload.completionFuture().join();
    }


    @Override
    public void shutdown() throws Exception {
        super.shutdown();
        if(transferManager != null){
            transferManager.close();
        }
    }

}
