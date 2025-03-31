package org.opensilex.fs.operation.thumbnail;

import org.opensilex.fs.operation.FileOperation;
import org.opensilex.fs.service.FileStorageConnection;

public abstract class ThumbnailOperation<F extends FileStorageConnection> implements FileOperation<F>{

    protected final int width;
    protected final int height;
    protected final String imgType;
    protected final F connection;

    protected ThumbnailOperation(int width, int height, String imgType, F connection) {
        this.width = width;
        this.height = height;
        this.imgType = imgType;
        this.connection = connection;
    }

    @Override
    public F getFileStorageConnection() {
        return connection;
    }
}
