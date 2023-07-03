package org.opensilex.utils;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ProcessUtils {

    private ProcessUtils() {
    }

    public static void checkErrorFromProcess(Process process)throws IOException {

        Objects.requireNonNull(process);
        if(! process.isAlive()){
            return;
        }

        try (InputStream errorStream = process.getErrorStream()) {
            byte[] errorBytes = IOUtils.toByteArray(errorStream);
            if (errorBytes != null && errorBytes.length > 0) {
                if (process.isAlive()) {
                    process.destroy();
                }
                throw new IOException(new String(errorBytes, StandardCharsets.UTF_8));
            }
        }
    }

}
