package org.opensilex.fs.operation;

import org.apache.commons.io.IOUtils;
import org.opensilex.fs.service.FileStorageConnection;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;


public interface FileOperation<F extends FileStorageConnection> {

    default boolean allowDirectInputRead(){
        return false;
    }

    default boolean allowRedirectOutput(){
        return false;
    }

    F getFileStorageConnection();

    /**
     * Execute the FS operation on the given file
     * @param filePath A path directly resolvable by the FS
     * @return output of operation
     */
    default byte[] execute(Path filePath) throws IOException{
        byte[] fileContent = getFileStorageConnection().readFileAsByteArray(filePath);
        return execute(fileContent);
    }

    byte[] execute(byte[] inputBytes) throws IOException;

    default byte[] execute(InputStream inputStream) throws IOException{
        // Read directly content from input stream and execute operation on the fly
        byte[] rawContent = IOUtils.toByteArray(inputStream);
        return execute(rawContent);
    }

}
