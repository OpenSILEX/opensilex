package org.opensilex.fs.operation.thumbnail;

import org.opensilex.fs.service.FileStorageConnection;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class JavaThumbnailOperation<F extends FileStorageConnection> extends ThumbnailOperation<F>{

    public JavaThumbnailOperation(int width, int height, String imgType, F connection) {
        super(width, height, imgType, connection);
    }

    @Override
    public byte[] execute(byte[] inputImgBytes) throws IOException {

        ByteArrayInputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;

        try {
            // read file and get image
            inputStream = new ByteArrayInputStream(inputImgBytes);
            BufferedImage sourceImage = ImageIO.read(inputStream);
            inputStream.close();

            // compute scaled image
            BufferedImage scaledImg = new BufferedImage(width, height, sourceImage.getType());
            Graphics2D graphics2D = scaledImg.createGraphics();
            graphics2D.drawImage(sourceImage, 0, 0, scaledImg.getWidth(), scaledImg.getHeight(), null);
            graphics2D.dispose();

            // write scaled image as byte array
            outputStream = new ByteArrayOutputStream();
            ImageIO.write(scaledImg, imgType, outputStream);
            outputStream.flush();
            byte[] resizedImageData = outputStream.toByteArray();

            outputStream.close();

            return resizedImageData;

        } catch (IOException e) {
            inputStream.close();
            if (outputStream != null) {
                outputStream.close();
            }
            throw e;
        }
    }

}
