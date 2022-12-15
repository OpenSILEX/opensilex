package org.opensilex.fs.s3;

import org.junit.Ignore;

@Ignore
public class S3TransferManagerConnectionTest extends S3FileStorageConnectionTest{

    public S3TransferManagerConnectionTest() throws ReflectiveOperationException {
        connection = getConnection(fs,"connection2");
    }
}
