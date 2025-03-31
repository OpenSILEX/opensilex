//package org.opensilex.fs.operation.checksum;
//
//import org.opensilex.fs.local.LocalFileSystemConnection;
//import org.opensilex.fs.operation.specialization.SpecializedFsOperationA;
//import org.opensilex.utils.ProcessExecutor;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.List;
//import java.util.Set;
//
///**
// * Optimized computing of file checksum operation which use the LINUX classic checksum commands
// * <ul>
// *     <li>The file is directly read by the linux command (it assumes that the file is locally accessible by the OpenSILEX user)</li>
// *     <li>The computed checksum is directly read without disk I/O by using process redirection</li>
// * </ul>
// */
//public class LinuxFsFileChecksumOperation extends SpecializedFsOperationA<LocalFileSystemConnection, FileCheckSumOperation> implements FileCheckSumOperation{
//
//    public static final Set<String> supportedChecksums = Set.of(
//            "sha1",
//            "sha2-256",
//            "sha2-384",
//            "sha2-512",
//            "sha3-256",
//            "sha3-384",
//            "sha3-512",
//            "md5"
//    );
//
//    private final ProcessExecutor processExecutor;
//    private final String algorithm;
//
//
//    public LinuxFsFileChecksumOperation(LocalFileSystemConnection fileStorage, FileCheckSumOperation operation, ProcessExecutor processExecutor) {
//        super(fileStorage, operation);
//        this.processExecutor = processExecutor;
//        this.algorithm = "";
//    }
//
//    public String execute(String algorithm, String filePath) throws IOException {
//
//        // direct read of the file from fs
//        if(! supportedChecksums.contains(algorithm)){
//            throw new IllegalArgumentException(
//                    String.format("Unknown checksum algorithm: %s, available are : %s", algorithm, supportedChecksums)
//            );
//        }
//
//        // Execute the openssl digest on specified file, parse the output and extract the first part (the sum)
//        var command = List.of("openssl", "dgst", "-" + algorithm, filePath, "|", "cut -d ' ' -f1");
//        byte[] checksum = processExecutor.execute(command);
//        return new String(checksum);
//    }
//
//    @Override
//    public boolean allowDirectInputRead() {
//        // The sha256sum command has direct access to the file
//        return true;
//    }
//
//    @Override
//    public boolean allowRedirectOutput() {
//        // The sha256sum command output can be read
//        return true;
//    }
//
//    @Override
//    public byte[] execute(String inputFilePath) throws IOException {
//        return new byte[0];
//    }
//
//    @Override
//    public byte[] execute(byte[] inputBytes) throws IOException {
//        return new byte[0];
//    }
//
//    @Override
//    public byte[] execute(InputStream inputStream) throws IOException {
//        return new byte[0];
//    }
//
//    @Override
//    public Set<String> getAlgorithms() {
//        return supportedChecksums;
//    }
//
//}
