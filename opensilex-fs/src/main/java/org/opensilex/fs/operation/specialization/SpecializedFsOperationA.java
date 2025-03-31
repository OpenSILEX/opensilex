package org.opensilex.fs.operation.specialization;

import org.opensilex.fs.operation.FileOperation;
import org.opensilex.fs.service.FileStorageConnection;

public abstract class SpecializedFsOperationA<F extends FileStorageConnection, O extends FileOperation<F>> implements SpecializedFsOperation<F,O> {

    private final F fsConnection;
    private final O operation;

    protected SpecializedFsOperationA(F fileStorage, O operation) {
        this.fsConnection = fileStorage;
        this.operation = operation;
    }

    @Override
    public F getFileStorageConnection() {
        return fsConnection;
    }

    @Override
    public O getOperation() {
        return operation;
    }

}
