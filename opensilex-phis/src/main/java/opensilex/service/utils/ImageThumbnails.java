package opensilex.service.utils;

import org.apache.commons.compress.utils.FileNameUtils;
import org.eclipse.rdf4j.common.io.FileUtil;
import org.opensilex.fs.local.LocalFileSystemConnection;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author rcolin
 * A utility class for creating picture thumbnail
 */
public class ImageThumbnails {

    private static ImageThumbnails _INSTANCE;
    private final Path THUMBNAIL_TMP_DIRECTORY;
    private final LocalFileSystemConnection localFsConnection;

    private final static String TMP_THUMBNAIL_DIR_NAME = "opensilex_thumbnail";

    private ImageThumbnails() throws IOException {
        THUMBNAIL_TMP_DIRECTORY = Files.createTempDirectory(TMP_THUMBNAIL_DIR_NAME);
        localFsConnection = new LocalFileSystemConnection();
    }

    public static ImageThumbnails getInstance() throws IOException {
        if (_INSTANCE == null) {
            _INSTANCE = new ImageThumbnails();
        }
        return _INSTANCE;
    }

    public enum THUMBNAIL_METHOD {

        /**
         * JAVA API
         */
        JAVA_API,

        /**
         * Convert command from ImageMagick
         */
        CONVERT_COMMAND
    }

    public byte[] getThumbnail(THUMBNAIL_METHOD method, Path srcImagePath, int scaledWidth, int scaledHeight) throws IOException {
        if (method.equals(THUMBNAIL_METHOD.CONVERT_COMMAND)) {
            return getThumbnailWithConvertCommand(srcImagePath, scaledWidth, scaledHeight);
        }
        return getThumbnailWithJavaAPI(srcImagePath, scaledWidth, scaledHeight);
    }

    public byte[] getThumbnail(Path srcImagePath, int scaledWidth, int scaledHeight) throws IOException {
        // TODO check if convert command is accessible through config at the startup
        return this.getThumbnail(THUMBNAIL_METHOD.JAVA_API, srcImagePath, scaledWidth, scaledHeight);
    }


    private void checkErrorFromProcess(Process process) throws IOException {

        InputStream errorStream = process.getErrorStream();
        try {
            byte[] errorBytes = errorStream.readAllBytes();
            if (errorBytes != null && errorBytes.length > 0) {
                errorStream.close();
                if (process.isAlive()) {
                    process.destroy();
                }
                throw new IOException(new String(errorBytes, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            errorStream.close();
            throw e;
        }
    }

    private static final String CONVERT_COMMAND = "convert";
    private static final String CONVERT_RESIZE_OPTION = "-resize";

    /**
     * Extra thumbnail args for JPEG specific convert optimization
     */
    private static final String CONVERT_JPEG_DEFINE_OPTION = "-define";
    private static final String CONVERT_JPEG_SIZE_OPTION = "jpeg:size=";

    private byte[] getThumbnailWithConvertCommand(Path srcImagePath, int scaledWidth, int scaledHeight) throws IOException {
        Process convertProcess = null;
        Path scaledImagePath = null;
        try {

            // create tmp file
            scaledImagePath = localFsConnection.createFile(Paths.get(THUMBNAIL_TMP_DIRECTORY.toString(), srcImagePath.getFileName().toString()));

            List<String> command = new ArrayList<>();
            command.add(CONVERT_COMMAND);

            // use convert optimization for JPEG file, could be very efficient for large jpeg file
            String fileExt = FileUtil.getFileExtension(srcImagePath.toString());
            if (fileExt.equals("jpeg") || fileExt.equals("jpg")) {
                int jpegWidth = scaledWidth * 2;
                int jpegHeight = scaledHeight * 2;
                command.addAll(Arrays.asList(CONVERT_JPEG_DEFINE_OPTION, CONVERT_JPEG_SIZE_OPTION + jpegWidth + "x" + jpegHeight));
            }

            command.addAll(Arrays.asList(
                    srcImagePath.toString(),
                    CONVERT_RESIZE_OPTION,
                    scaledWidth + "x" + scaledHeight,
                    scaledImagePath.toString()
            ));

            convertProcess = new ProcessBuilder().command(command).start();
            checkErrorFromProcess(convertProcess);

            byte[] scaledImageContent = localFsConnection.readFileAsByteArray(scaledImagePath);
            localFsConnection.delete(scaledImagePath);

            return scaledImageContent;

        } catch (Exception e) {
            if (convertProcess != null && convertProcess.isAlive()) {
                convertProcess.destroy();
            }
            if (scaledImagePath != null) {
                localFsConnection.delete(scaledImagePath);
            }
            throw e;
        }
    }

    /**
     * @param srcImagePath path to source picture to transform
     *                     //     * @param scaleRatio ratio used to determine the thumbnail weight and height according the source image
     * @return the content of the created thumbnail
     */
    private byte[] getThumbnailWithJavaAPI(Path srcImagePath, int scaledWidth, int scaledHeight) throws IOException {

        ByteArrayInputStream bais = null;
        ByteArrayOutputStream baos = null;

        try {
            // read file and get image
            byte[] fileContent = localFsConnection.readFileAsByteArray(srcImagePath);
            bais = new ByteArrayInputStream(fileContent);
            BufferedImage sourceImage = ImageIO.read(bais);
            bais.close();

            // compute scaled image
            BufferedImage thumbImg = new BufferedImage(scaledWidth, scaledHeight, sourceImage.getType());
            Graphics2D graphics2D = thumbImg.createGraphics();
            graphics2D.drawImage(sourceImage, 0, 0, thumbImg.getWidth(), thumbImg.getHeight(), null);
            graphics2D.dispose();

            // write scaled image as byte array
            baos = new ByteArrayOutputStream();
            ImageIO.write(thumbImg, FileNameUtils.getExtension(srcImagePath.toString()), baos);
            baos.flush();
            byte[] thumbnailData = baos.toByteArray();

            baos.close();

            return thumbnailData;

        } catch (IOException e) {
            if (bais != null) {
                bais.close();
            }
            if (baos != null) {
                baos.close();
            }
            throw e;
        }
    }

}
