package org.opensilex.core.data.api;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.*;
import org.jfree.chart.*;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeriesCollection;
import javax.imageio.ImageIO;
import org.jfree.chart.axis.NumberAxis;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
/**
 * @author efernandez A utility class to convert DX spectrum file to picture
 */
public class DXFileConverter {

    private static DXFileConverter _INSTANCE;

    public static DXFileConverter getInstance() throws IOException {
        if (_INSTANCE == null) {
            _INSTANCE = new DXFileConverter();
        }
        return _INSTANCE;
    }

    /**
     * Converts the content of a DX spectrum file into a PNG image.
     *
     * The method extracts spectral metadata (FIRSTX, LASTX, DELTAX, YFACTOR, TITLE,
     * XUNITS, YUNITS) and XY data from the DX content, builds a dataset, and renders
     * a chart as a buffered image.
     *
     * @param dxData the byte array containing the DX file content, encoded in UTF-8
     * @return a byte array representing the generated PNG image
     * @throws IOException if an error occurs while reading the DX data or generating the image
     */
    public byte[] convertDXToImage(byte[] dxData) throws IOException {
        String dxString = new String(dxData, StandardCharsets.UTF_8);

        Pattern firstXPattern = Pattern.compile("##FIRSTX=(\\d+\\.?\\d*)");
        Pattern lastXPattern = Pattern.compile("##LASTX=(\\d+\\.?\\d*)");
        Pattern deltaXPattern = Pattern.compile("##DELTAX=(\\d+\\.?\\d*)");
        Pattern factorYPattern = Pattern.compile("##YFACTOR=(.*)");
        Pattern titlePattern = Pattern.compile("##TITLE=(.*)");
        Pattern unitXPattern = Pattern.compile("##XUNITS=(.*)");
        Pattern unitYPattern = Pattern.compile("##YUNITS=(.*)");
        Pattern xyDataPattern = Pattern.compile("##XYDATA=(.*?)##END=", Pattern.DOTALL);

        double firstX = 0.0;
        double lastX = 0.0;
        double deltaX = 0.0;
        double factorY = 0.0;
        String title = "";
        String unitX = "";
        String unitY = "";
        String xyData = "";

        //Extract metadata from DX file
        firstX = extractDouble(firstXPattern, dxString, 0.0);
        lastX = extractDouble(lastXPattern, dxString, 0.0);
        deltaX = extractDouble(deltaXPattern, dxString, 0.0);
        factorY = extractDouble(factorYPattern, dxString, 0.0);

        title = extractString(titlePattern, dxString, "");
        unitX = extractString(unitXPattern, dxString, "");
        unitY = extractString(unitYPattern, dxString, "");
        xyData = extractString(xyDataPattern, dxString, "");

        BufferedImage graphImage = generateGraphImage(firstX, deltaX, title, unitX, unitY, xyData, factorY);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(graphImage, "png", baos);
        byte[] imageData = baos.toByteArray();

        baos.close();

        return imageData;
    }

    /**
     * Generates a graphical representation of the DX (JCAMP Spectroscopic Data Exchange Format) data as a line chart.
     *
     * This method is used internally to create the spectrum chart image based on parsed
     * DX parameters.
     *
     * @param firstX  the first X value from the DX metadata
     * @param deltaX  the increment between X values
     * @param title   the title of the spectrum (from DX metadata)
     * @param unitX   the unit of the X axis
     * @param unitY   the unit of the Y axis
     * @param xyData  the string block containing XY data pairs
     * @param factorY scaling factor to apply to Y values
     * @return a {@link BufferedImage} containing the rendered line chart
     */
    private BufferedImage generateGraphImage(double firstX, double deltaX, String title, String unitX, String unitY, String xyData, double factorY) {
        XYSeries series = new XYSeries("Spectrum");
        String[] lines = xyData.split("\n");

        for (String line : lines) {
            String[] values = line.trim().split("\\s+");
            
            if (values.length >= 2) {
                double x = Double.parseDouble(values[0]);
                for (int i = 1; i < values.length; i++) {
                    double y = Double.parseDouble(values[i]) * factorY;
                    series.add(x, y);
                    x += deltaX;
                }
            }
        }
        XYSeriesCollection dataset = new XYSeriesCollection(series);
    
        JFreeChart chart = ChartFactory.createXYLineChart(
                title,                 
                unitX,         
                unitY,         
                dataset,      
                PlotOrientation.VERTICAL,
                true,               
                true,             
                false        
        );

        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setPaint(Color.BLACK);
        chart.getRenderingHints().put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        chart.getRenderingHints().put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        NumberAxis yAxis = new NumberAxis();
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        DecimalFormat format = new DecimalFormat("0.##", symbols);
        yAxis.setNumberFormatOverride(format);
        yAxis.setLabel(unitY); 
        chart.getXYPlot().setRangeAxis(yAxis);

        int width = 400;
        int height = 300;
        BufferedImage graphImage = chart.createBufferedImage(width, height);

        return graphImage;
    }

    private double extractDouble(Pattern pattern, String text, double defaultValue) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return defaultValue;
    }

    private String extractString(Pattern pattern, String text, String defaultValue) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return defaultValue;
    }
}