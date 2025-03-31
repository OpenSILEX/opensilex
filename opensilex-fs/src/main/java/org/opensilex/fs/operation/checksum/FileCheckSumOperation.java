package org.opensilex.fs.operation.checksum;

import org.opensilex.fs.operation.FileOperation;
import org.opensilex.fs.service.FileStorageConnection;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class FileCheckSumOperation<F extends FileStorageConnection> implements FileOperation<F> {

    private final MessageDigest messageDigest;
    private final F connection;

    public FileCheckSumOperation(String algorithm, F connection) throws NoSuchAlgorithmException {
        this.messageDigest = MessageDigest.getInstance(algorithm);
        this.connection = connection;
    }

    @Override
    public F getFileStorageConnection() {
        return connection;
    }

    @Override
    public byte[] execute(byte[] inputBytes) throws IOException {
        return messageDigest.digest(inputBytes);
    }

}
