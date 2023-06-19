package org.opensilex.utils.unix;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author rcolin
 */
public class UnixFileUtils {

    private UnixFileUtils() {
    }

    /**
     * Check that if the file only has permissions which are in the given expected permissions set
     *
     * @param filePath            the file path
     * @param errorTitle          An error title passed for a more explicit error message in case of permissions error.
     * @param expectedPermissions The set of permissions
     * @throws IOException       if files permissions can't be determined.
     * @throws SecurityException if the file has an unexpected permission
     */
    public static void checkPermissions(Path filePath,
                                        String errorTitle,
                                        Set<PosixFilePermission> expectedPermissions) throws IOException, SecurityException {

        Objects.requireNonNull(filePath);
        Objects.requireNonNull(expectedPermissions);
        if (StringUtils.isEmpty(errorTitle)) {
            throw new IllegalArgumentException("Null or empty errorTitle : " + errorTitle);
        }

        // Compute permissions set difference
        Set<PosixFilePermission> filePermissions = Files.getPosixFilePermissions(filePath);
        Set<PosixFilePermission> forbiddenPermissions = Sets.difference(filePermissions, expectedPermissions);

        // Generate Error with forbidden permissions list
        if (!forbiddenPermissions.isEmpty()) {
            String permissionsStr = forbiddenPermissions.stream()
                    .map(PosixFilePermission::toString)
                    .collect(Collectors.joining("\n"));

            throw new SecurityException(
                    String.format("[%s] Permissions for file %s are too open: %n %s", errorTitle, filePath, permissionsStr)
            );
        }
    }

    private static final Set<PosixFilePermission> OWNER_PERMISSIONS = Sets.newHashSet(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE
    );

    /**
     * Check that the credentialFile is readable by OpenSILEX user and that other users have no permissions (READ,WRITE,EXEC)
     *
     * @param file             the file to check
     * @param errorTitle       an error msg used in case of Security error for better explainability
     * @param checkPermissions if true, this method check that the file has only owner (READ,WRITE AND EXEC) permissions
     * @param checkOwnership   if true, this method check that the user which run the OpenSILEX process is the owner of the file
     * @throws IOException       if files permissions can't be determined.
     * @throws SecurityException if checkPermissions or checkOwnership is true and that corresponding security assertions are violated
     */
    public static void checkFilePermissionsAndOwnership(File file,
                                                        String errorTitle,
                                                        boolean checkPermissions,
                                                        boolean checkOwnership) throws IOException, SecurityException {

        Objects.requireNonNull(file);
        if (StringUtils.isEmpty(errorTitle)) {
            throw new IllegalArgumentException("Null or empty errorTitle : " + errorTitle);
        }

        // check if files exists and is readable
        if (!file.exists()) {
            throw new IllegalArgumentException(String.format("[%s] File not found %s", errorTitle, file.getPath()));
        }
        if (!file.canRead()) {
            throw new IllegalArgumentException(String.format("[%s] File can't be read %s", errorTitle, file.getPath()));
        }

        Path filePath = file.toPath();

        // Check UNIX permissions, ensure that GROUP AND OTHER have no READ/WRITE/EXECUTE permissions
        if (checkPermissions) {
            UnixFileUtils.checkPermissions(filePath, errorTitle, OWNER_PERMISSIONS);
        }

        // check if current java-process user is the owner of the credentials file
        if (!checkOwnership) {
            UserPrincipal fileOwner = Files.getOwner(filePath);
            String javaProcessUser = System.getProperty("user.name");

            if (!fileOwner.getName().equals(javaProcessUser)) {
                throw new SecurityException(String.format(
                        "[%s] The file path should be owned by the OpenSILEX application system user : %s. File owner is %s", errorTitle, javaProcessUser, fileOwner.getName())
                );
            }

        }


    }

}
