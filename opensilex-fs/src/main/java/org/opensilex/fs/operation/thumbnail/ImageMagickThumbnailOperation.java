package org.opensilex.fs.operation.thumbnail;

import org.opensilex.fs.service.FileStorageConnection;
import org.opensilex.utils.ProcessExecutor;

import java.io.IOException;

public class ImageMagickThumbnailOperation<F extends FileStorageConnection> extends ThumbnailOperation<F> {

    private static final String CONVERT_COMMAND = "convert";
    private static final String CONVERT_RESIZE_OPTION = "-resize";

    /**
     * Extra args for JPEG specific convert optimization
     */
    private static final String CONVERT_JPEG_DEFINE_OPTION = "-define";
    private static final String CONVERT_JPEG_SIZE_OPTION = "jpeg:size=";

    private final ProcessExecutor processExecutor;

    public ImageMagickThumbnailOperation(int outputWidth, int outputHeight, String imgType, F connection) throws IOException {
        super(outputWidth, outputHeight, imgType, connection);
        processExecutor = new ProcessExecutor();

        // check package availability
        processExecutor.throwIfStderr(CONVERT_COMMAND);
    }

    @Override
    public byte[] execute(byte[] inputBytes) throws IOException {
        // write file content to convert process stdin

        // read convert process stdout
        return new byte[0];
    }
}
