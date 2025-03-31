package org.opensilex.core.data.api;

import org.apache.tika.Tika;
import org.opensilex.utils.ProcessExecutor;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author rcolin A utility class for resizing picture
 */
public class ImageResizer {

    private static ImageResizer _INSTANCE;
    private final Path RESIZED_PICTURE_TMP_DIR;

    // use Tika for JPEG picture recognition
    private final Tika tika;
    private final static String JPEG_MIME_TYPE = "image/jpeg";
    private final static String JPG_MIME_TYPE = "image/jpg";
    private final static String IMAGE_IO_JPEG_TYPE = "JPEG";

    private final ProcessExecutor processExecutor;

    private final RESIZE_METHOD defaultResizeMethod;

    private ImageResizer() throws IOException {

        RESIZED_PICTURE_TMP_DIR = Files.createTempDirectory("opensilex_resized_picture");
        RESIZED_PICTURE_TMP_DIR.toFile().deleteOnExit();

        tika = new Tika();
        processExecutor = new ProcessExecutor();

        // determine if we can use the command convert, else use the java API based method
        RESIZE_METHOD resizeMethod;
        Process testConvertProcess = null;
        try {
            testConvertProcess = new ProcessBuilder(CONVERT_COMMAND, "-version").start();
            processExecutor.throwIfStderr(testConvertProcess);
            resizeMethod = RESIZE_METHOD.CONVERT_COMMAND;
        } catch (Exception e) {
            if (testConvertProcess != null && testConvertProcess.isAlive()) {
                testConvertProcess.destroy();
            }
            resizeMethod = RESIZE_METHOD.JAVA_API;
        }
        defaultResizeMethod = resizeMethod;
    }

    public static ImageResizer getInstance() throws IOException {
        if (_INSTANCE == null) {
            _INSTANCE = new ImageResizer();
        }
        return _INSTANCE;
    }

    public enum RESIZE_METHOD {

        /**
         * JAVA API
         */
        JAVA_API,
        /**
         * Convert command from ImageMagick
         *
         * @see <a href="https://imagemagick.org/script/convert.php"></a>
         */
        CONVERT_COMMAND
    }

    public byte[] resize(RESIZE_METHOD method, byte[] img, int scaledWidth, int scaledHeight) throws IOException {
        if (method.equals(RESIZE_METHOD.CONVERT_COMMAND)) {
            return getResizedImageWithConvertCmd(img, scaledWidth, scaledHeight);
        }
        return getResizedImageWithJavaAPI(img, scaledWidth, scaledHeight);
    }

    public byte[] resize(byte[] img, int scaledWidth, int scaledHeight) throws IOException {
        return this.resize(defaultResizeMethod, img, scaledWidth, scaledHeight);
    }

    private static final String CONVERT_COMMAND = "convert";
    private static final String CONVERT_RESIZE_OPTION = "-resize";

    /**
     * Extra args for JPEG specific convert optimization
     */
    private static final String CONVERT_JPEG_DEFINE_OPTION = "-define";
    private static final String CONVERT_JPEG_SIZE_OPTION = "jpeg:size=";

    /**
     * @apiNote This implementation
     */
    private byte[] getResizedImageWithConvertCmd(byte[] img, int scaledWidth, int scaledHeight) throws IOException {
        Process convertProcess = null;
        Path srcImagePath = null;
        Path scaledImagePath = null;
        try {
            /* (#TODO #optimisation-0) : Allow to directly perform the fs call to get data (with redirection) or command
            *
            *
            *(#TODO #optimisation-irods-0) : Execute thumbnail of fs (less I/O, more CPU usage on server)
            */
            srcImagePath = Files.createTempFile(RESIZED_PICTURE_TMP_DIR, null, null);
            Files.write(srcImagePath, img);

            // create tmp file
            scaledImagePath = Files.createTempFile(RESIZED_PICTURE_TMP_DIR, null, null);

            List<String> command = new ArrayList<>();
            command.add(CONVERT_COMMAND);

            // use convert optimization for JPEG file, could be very efficient for large jpeg file
            String fileExt = tika.detect(img);
            if (fileExt.equals(JPEG_MIME_TYPE) || fileExt.equals(JPG_MIME_TYPE)) {
                int jpegWidth = scaledWidth * 2;
                int jpegHeight = scaledHeight * 2;
                command.addAll(Arrays.asList(CONVERT_JPEG_DEFINE_OPTION, CONVERT_JPEG_SIZE_OPTION + jpegWidth + "x" + jpegHeight));
            }

            /* (#TODO #optimisation-1) : Redirect command output instead of writing inside tmp file
            /* - (no drive write+read)
            /* - no additional impact on memory allocation (space/time) since the thumbnail has to be read inside a buffer anyway
            *
            * (#TODO #optimisation-2) : implement thumbnail cache (ex n:10000, size: 1G)
            * - no fs read (no disk read from fs-server, no network transfer)
            */

            command.addAll(Arrays.asList(
                    srcImagePath.toString(),
                    CONVERT_RESIZE_OPTION,
                    scaledWidth + "x" + scaledHeight,
                    scaledImagePath.toString()
            ));

            convertProcess = new ProcessBuilder().command(command).start();
            processExecutor.execute(false, command.toArray(new String[0]));

            return Files.readAllBytes(scaledImagePath);

        } finally {
            if (convertProcess != null && convertProcess.isAlive()) {
                convertProcess.destroy();
            }
            if(srcImagePath != null){
                Files.deleteIfExists(srcImagePath);
            }
            if(scaledImagePath != null){
                Files.deleteIfExists(scaledImagePath);
            }
        }
    }

    /**
     * @param img content of the source picture to transform
     * @return the content of the created resized image
     */
    private byte[] getResizedImageWithJavaAPI(byte[] img, int scaledWidth, int scaledHeight) throws IOException {

        ByteArrayInputStream bais = null;
        ByteArrayOutputStream baos = null;

        try {
            // read file and get image
            bais = new ByteArrayInputStream(img);
            BufferedImage sourceImage = ImageIO.read(bais);
            bais.close();

            // compute scaled image
            BufferedImage scaledImg = new BufferedImage(scaledWidth, scaledHeight, sourceImage.getType());
            Graphics2D graphics2D = scaledImg.createGraphics();
            graphics2D.drawImage(sourceImage, 0, 0, scaledImg.getWidth(), scaledImg.getHeight(), null);
            graphics2D.dispose();

            // write scaled image as byte array
            baos = new ByteArrayOutputStream();
            String fileExt = tika.detect(img).replace("image/", "");            
            ImageIO.write(scaledImg, fileExt, baos);
            baos.flush();
            byte[] resizedImageData = baos.toByteArray();

            baos.close();

            return resizedImageData;

        } catch (IOException e) {
            bais.close();
            if (baos != null) {
                baos.close();
            }
            throw e;
        }
    }

}
