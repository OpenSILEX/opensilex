package org.opensilex.fs.s3;

public class S3TransferManagerConnectionTest extends S3FileStorageConnectionTest{

    public S3TransferManagerConnectionTest() throws ReflectiveOperationException {
        connection = getConnection(fs,"connection2");
    }
}
