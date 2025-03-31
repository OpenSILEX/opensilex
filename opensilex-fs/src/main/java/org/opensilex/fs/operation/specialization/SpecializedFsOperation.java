package org.opensilex.fs.operation.specialization;

import org.opensilex.fs.operation.FileOperation;
import org.opensilex.fs.service.FileStorageConnection;

public interface SpecializedFsOperation<F extends FileStorageConnection, O extends FileOperation<F>> extends FileOperation<F> {

    F getFileStorageConnection();
    O getOperation();
}
