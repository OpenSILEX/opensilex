package org.opensilex.core.data.api;

import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;

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

    private final RESIZE_METHOD defaultResizeMethod;

    private ImageResizer() throws IOException {

        RESIZED_PICTURE_TMP_DIR = Files.createTempDirectory("opensilex_resized_picture");
        RESIZED_PICTURE_TMP_DIR.toFile().deleteOnExit();

        tika = new Tika();

        // determine if we can use the command convert, else use the java API based method
        RESIZE_METHOD resizeMethod;
        Process testConvertProcess = null;
        try {
            testConvertProcess = new ProcessBuilder(CONVERT_COMMAND, "-version").start();
            checkErrorFromProcess(testConvertProcess);
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

    private void checkErrorFromProcess(Process process) throws IOException {

        InputStream errorStream = process.getErrorStream();
        try {
            byte[] errorBytes = IOUtils.toByteArray(errorStream);
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
     * Extra args for JPEG specific convert optimization
     */
    private static final String CONVERT_JPEG_DEFINE_OPTION = "-define";
    private static final String CONVERT_JPEG_SIZE_OPTION = "jpeg:size=";

    private byte[] getResizedImageWithConvertCmd(byte[] img, int scaledWidth, int scaledHeight) throws IOException {
        Process convertProcess = null;
        Path srcImagePath = null;
        Path scaledImagePath = null;
        try {
            srcImagePath = Files.createTempFile(Paths.get(RESIZED_PICTURE_TMP_DIR.toString()), null, null);

            // create tmp file
            scaledImagePath = Files.createTempFile(Paths.get(RESIZED_PICTURE_TMP_DIR.toString()), null, null);
            Files.write(scaledImagePath, img);

            List<String> command = new ArrayList<>();
            command.add(CONVERT_COMMAND);

            // use convert optimization for JPEG file, could be very efficient for large jpeg file
            String fileExt = tika.detect(img);

            if (fileExt.equals(JPEG_MIME_TYPE) || fileExt.equals(JPG_MIME_TYPE)) {
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
            ImageIO.write(scaledImg, IMAGE_IO_JPEG_TYPE, baos);
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
